# TradeSim Engineering Notes

This file is the maintainer's incident analysis and recovery patch. It is intentionally separate from the candidate-facing investigation brief.

## System Architecture

The embedded HTTP server serves the static dashboard and the JSON API. MarketDataRepository owns deterministic price series. PolicyEngine selects an execution policy, and each strategy returns an immutable BacktestResult with fills and telemetry. The browser renders only backend-provided prices, transactions, and metrics.

## Bug Analysis

### BUG 1 — Single trade prefix minimum

- **File / class / method:** src/main/java/com/tradesim/strategy/SingleTradeStrategy.java, SingleTradeStrategy.execute
- **Approximate line:** 8
- **Root cause:** The current price replaces the running minimum before the selling opportunity is evaluated. The computed spread is therefore always zero, and no valid buy day is retained.
- **Why it occurs:** The state update order loses the best purchase price from earlier sessions.
- **Expected:** Compare today's close with the minimum close observed strictly before today; preserve the day of that minimum.
- **Observed:** The strategy reports no transaction for a series with an obvious later recovery.
- **State transition:** Update the best sale opportunity from the prior minimum, then update the minimum for future sessions.
- **Fix:** Evaluate the spread first and update both minimum price and its day afterward.

### BUG 2 — Unlimited transaction boundary

- **File / class / method:** src/main/java/com/tradesim/strategy/UnlimitedStrategy.java, UnlimitedStrategy.execute
- **Approximate line:** 8
- **Root cause:** The strategy discards the first complete buy/sell interval whenever there is more than one interval.
- **Why it occurs:** A stale boundary workaround removes a valid pair from the strategy support result.
- **Expected:** Preserve every rising interval detected by the shared transaction boundary scan.
- **Observed:** Profit and transaction count omit the first profitable cycle.
- **State transition:** Each completed SELL closes the current cycle before a later BUY begins.
- **Fix:** Return the full non-overlapping interval list.

### BUG 3 — Transaction-limited dynamic programming state

- **File / class / method:** src/main/java/com/tradesim/strategy/LimitedTransactionStrategy.java, LimitedTransactionStrategy.execute
- **Approximate line:** 22
- **Root cause:** The terminal transaction dimension is capped at one even when the caller configured a larger limit.
- **Why it occurs:** The DP computes states for all permitted transaction counts but the result selection ignores those states.
- **Expected:** Reconstruct from the highest permitted terminal state, bounded by available days and the configured limit.
- **Observed:** A second disjoint profitable cycle cannot contribute to the selected result.
- **State transition:** Each outer DP row represents another completed transaction; a row may be chosen only after its SELL state is reached.
- **Fix:** Select k, the permitted transaction count, for result reconstruction.

### BUG 4 — Fee accounting

- **File / class / method:** src/main/java/com/tradesim/strategy/FeeStrategy.java, FeeStrategy.execute
- **Approximate line:** 11
- **Root cause:** Each SELL is charged once by StrategySupport and charged a second time while the strategy rebuilds the transaction ledger.
- **Why it occurs:** Fees are applied in both the policy's accounting pass and its post-processing pass.
- **Expected:** A completed transaction pays exactly one configured fee; gross profit minus that fee equals net profit.
- **Observed:** SELL records, total fees, and net profit show two fees per completed transaction.
- **State transition:** Apply one fee on each completed SELL and keep the same amount in the SELL fill, aggregate telemetry, and final profit.
- **Fix:** Return the fee-aware result directly from StrategySupport.

### BUG 5 — Cooldown off by one

- **File / class / method:** src/main/java/com/tradesim/strategy/CooldownStrategy.java, CooldownStrategy.execute
- **Approximate line:** 10
- **Root cause:** The eligibility check uses a strict comparison, making the first cooldown day appear eligible.
- **Why it occurs:** The sell day and cooldown day are not both excluded from re-entry.
- **Expected:** With one cooldown day after a SELL on day N, day N+1 is unavailable and the earliest BUY is day N+2.
- **Observed:** A rising opportunity can trigger a BUY on day N+1.
- **State transition:** Re-entry is blocked through lastSell + cooldownDays; the next allowed index is strictly greater than that boundary.
- **Fix:** Use an inclusive boundary check.

### BUG 6 — PolicyEngine state isolation

- **File / class / method:** src/main/java/com/tradesim/policy/PolicyEngine.java, runBacktest and reset
- **Approximate line:** 8
- **Root cause:** A mutable fee ledger is shared by all requests handled by the engine and prior fee totals are applied to later results.
- **Why it occurs:** Request-specific accounting is stored in a long-lived service field rather than the current backtest context.
- **Expected:** Every run is determined only by its ticker, strategy, and rules; repeating a request yields identical fills and telemetry.
- **Observed:** The next fee or composite run inherits stale fees and altered portfolio telemetry.
- **State transition:** Create all mutable execution state for a single invocation and discard it when that invocation ends.
- **Fix:** Remove the shared execution ledger and the result mutation based on prior requests.

## Socratic Hints

### BUG 1

1. Which price history is available before the current session closes?
2. Inspect whether the running minimum changes before the day's spread is evaluated.
3. Can the current close be the purchase price for a sale at that same close?
4. Keep the minimum's day and update the minimum only after evaluating today's opportunity.

### BUG 2

1. How many independent rising runs exist in the sample market?
2. Compare detected buy/sell pairs with the output transaction count.
3. Inspect the code between interval detection and result construction.
4. Preserve the entire interval list returned by the boundary scanner.

### BUG 3

1. What does each row of the DP table represent?
2. Which transaction dimension is selected when building the result?
3. Does that dimension honor the configured maximum K?
4. Reconstruct from k, the bounded transaction limit, rather than clamping it to one.

### BUG 4

1. At which event should the exchange fee become realized?
2. Compare the SELL fill fee with aggregate fees paid.
3. Trace whether the fee-aware strategy rebuilds values already returned by its helper.
4. Use one accounting pass and return its result without charging the SELL again.

### BUG 5

1. If a sell occurs on day N, which day is reserved by a one-day cooldown?
2. Check whether the comparison includes the cooldown boundary.
3. Does the code allow equality at lastSell + cooldownDays?
4. Make that boundary unavailable so a new BUY requires a strictly later day.

### BUG 6

1. Run the same fee policy twice with a different policy between the runs.
2. Compare fees, net profit, and portfolio values from the first and repeated runs.
3. Search PolicyEngine for mutable fields read while constructing result telemetry.
4. Remove the shared ledger and keep mutable execution state local to one run.

## Exact Unified Diff

Apply this patch from the repository root with git apply. It corrects the six seeded defects.

~~~diff
diff --git a/src/main/java/com/tradesim/policy/PolicyEngine.java b/src/main/java/com/tradesim/policy/PolicyEngine.java
index 86f7bbb..2213991 100644
--- a/src/main/java/com/tradesim/policy/PolicyEngine.java
+++ b/src/main/java/com/tradesim/policy/PolicyEngine.java
@@ -5,24 +5,16 @@ import com.tradesim.strategy.*;
 import java.util.*;
 public final class PolicyEngine {
     private final MarketDataRepository repository=new MarketDataRepository();
-    private final Map<String,Double> executionLedger=new HashMap<>(); // seeded state-isolation defect
     public List<String> tickers(){return repository.tickers();}
     public BacktestResult runBacktest(String ticker,String strategy,TradingRules rules){
         MarketData market=repository.get(ticker); TradingStrategy selected=create(strategy);
         BacktestResult result=selected.execute(market,rules);
-        if(strategy.equals("fee")||strategy.equals("combined")){
-            double carried=executionLedger.getOrDefault(strategy,0.0)+result.telemetry().feesPaid();
-            executionLedger.put(strategy,carried);
-            double extra=carried-result.telemetry().feesPaid();
-            if(extra!=0){var t=result.telemetry();var changed=new BacktestResult.Telemetry(t.daysProcessed(),t.buySignals(),t.sellSignals(),t.feesPaid()+extra,t.cooldownDaysObserved(),t.peakPortfolioValue()-extra,t.finalPortfolioValue()-extra,t.grossProfit(),t.netProfit()-extra);
-                result=new BacktestResult(result.strategy(),result.totalProfit()-extra,result.transactionCount(),result.maxDrawdown(),"anomaly",result.transactions(),changed);}
-        }
         return result;
     }
     public List<BacktestResult> compare(String ticker,TradingRules rules){
         List<BacktestResult> out=new ArrayList<>(); for(String id:List.of("single","unlimited","limited","fee","cooldown","combined"))out.add(runBacktest(ticker,id,rules)); return out;
     }
-    public void reset(){executionLedger.clear();}
+    public void reset(){}
     private TradingStrategy create(String id){return switch(id){
         case "single"->new SingleTradeStrategy(); case "unlimited"->new UnlimitedStrategy(); case "limited"->new LimitedTransactionStrategy(2);
         case "fee"->new FeeStrategy(); case "cooldown"->new CooldownStrategy(); case "combined"->new CombinedStrategy();
diff --git a/src/main/java/com/tradesim/strategy/CooldownStrategy.java b/src/main/java/com/tradesim/strategy/CooldownStrategy.java
index 3e66ff7..a441dba 100644
--- a/src/main/java/com/tradesim/strategy/CooldownStrategy.java
+++ b/src/main/java/com/tradesim/strategy/CooldownStrategy.java
@@ -6,8 +6,8 @@ public final class CooldownStrategy implements TradingStrategy {
     public BacktestResult execute(MarketData market, TradingRules rules) {
         List<Double> p=market.prices(); List<int[]> pairs=new ArrayList<>(); int i=0,lastSell=-1;
         while(i<p.size()-1){while(i<p.size()-1&&p.get(i+1)<=p.get(i))i++; int buy=i;
-            if(buy<lastSell+rules.cooldownDays()) { i++; continue; }
+            if(buy<=lastSell+rules.cooldownDays()) { i++; continue; }
             while(i<p.size()-1&&p.get(i+1)>p.get(i))i++; if(i>buy){pairs.add(new int[]{buy,i});lastSell=i;} }
-        return StrategySupport.anomaly(StrategySupport.result(getName(),market,pairs,0,rules.cooldownDays()));
+        return StrategySupport.result(getName(),market,pairs,0,rules.cooldownDays());
     }
 }
diff --git a/src/main/java/com/tradesim/strategy/FeeStrategy.java b/src/main/java/com/tradesim/strategy/FeeStrategy.java
index f7add7a..04ffa0c 100644
--- a/src/main/java/com/tradesim/strategy/FeeStrategy.java
+++ b/src/main/java/com/tradesim/strategy/FeeStrategy.java
@@ -5,12 +5,7 @@ public final class FeeStrategy implements TradingStrategy {
     public String getName(){return "Fee-Aware Execution Policy";}
     public BacktestResult execute(MarketData market, TradingRules rules) {
         List<int[]> pairs=feeAware(market.prices(),rules.transactionFee());
-        BacktestResult once=StrategySupport.result(getName(),market,pairs,rules.transactionFee(),0);
-        List<Trade> trades=new ArrayList<>(); double feeTotal=0;
-        for(Trade t:once.transactions()){double f=t.fee(); if(t.action().equals("SELL")){f+=rules.transactionFee();feeTotal+=f;trades.add(new Trade(t.action(),t.day(),t.price(),f,t.realizedProfit()-rules.transactionFee()));} else trades.add(t);}
-        double net=once.totalProfit()-feeTotal+once.telemetry().feesPaid();
-        var tele=new BacktestResult.Telemetry(once.telemetry().daysProcessed(),once.telemetry().buySignals(),once.telemetry().sellSignals(),feeTotal,0,100000+net,100000+net,once.telemetry().grossProfit(),net);
-        return new BacktestResult(getName(),net,pairs.size(),once.maxDrawdown(),"anomaly",trades,tele);
+        return StrategySupport.result(getName(),market,pairs,rules.transactionFee(),0);
     }
     private record State(double value,List<int[]> pairs,int buyDay) { }
     private List<int[]> feeAware(List<Double> prices,double fee) {
diff --git a/src/main/java/com/tradesim/strategy/LimitedTransactionStrategy.java b/src/main/java/com/tradesim/strategy/LimitedTransactionStrategy.java
index fa0795f..74ea067 100644
--- a/src/main/java/com/tradesim/strategy/LimitedTransactionStrategy.java
+++ b/src/main/java/com/tradesim/strategy/LimitedTransactionStrategy.java
@@ -18,13 +18,12 @@ public final class LimitedTransactionStrategy implements TradingStrategy {
                 if(candidate>dp[t][d]) { dp[t][d]=candidate; buys[t][d]=b; sells[t][d]=d; }
             }
         }
-        // Seeded DP state defect: the terminal state is capped at a single completed cycle.
-        int chosen=Math.min(1,k);
+        int chosen=k;
         List<int[]> pairs=new ArrayList<>();
         for(int t=chosen,d=n-1;t>0&&d>=0;) {
             int b=buys[t][d],s=sells[t][d];
             if(s>b){pairs.add(0,new int[]{b,s});t--;d=b-1;} else d--;
         }
-        return StrategySupport.anomaly(StrategySupport.result(getName(),market,pairs,0,0));
+        return StrategySupport.result(getName(),market,pairs,0,0);
     }
 }
diff --git a/src/main/java/com/tradesim/strategy/SingleTradeStrategy.java b/src/main/java/com/tradesim/strategy/SingleTradeStrategy.java
index 27834e1..4c3f03a 100644
--- a/src/main/java/com/tradesim/strategy/SingleTradeStrategy.java
+++ b/src/main/java/com/tradesim/strategy/SingleTradeStrategy.java
@@ -5,8 +5,11 @@ public final class SingleTradeStrategy implements TradingStrategy {
     public String getName(){return "Single Execution Cycle";}
     public BacktestResult execute(MarketData market, TradingRules rules) {
         List<Double> p=market.prices(); if(p.size()<2) return StrategySupport.result(getName(),market,List.of(),0,0);
-        double min=p.get(0), best=0; int buy=-1,sell=-1;
-        for(int i=1;i<p.size();i++) { min=p.get(i); if(p.get(i)-min>best){best=p.get(i)-min; buy=i-1; sell=i;} }
-        return StrategySupport.anomaly(StrategySupport.result(getName(),market,buy<0?List.of():List.of(new int[]{buy,sell}),0,0));
+        double min=p.get(0), best=0; int minDay=0,buy=-1,sell=-1;
+        for(int i=1;i<p.size();i++) {
+            if(p.get(i)-min>best){best=p.get(i)-min; buy=minDay; sell=i;}
+            if(p.get(i)<min){min=p.get(i);minDay=i;}
+        }
+        return StrategySupport.result(getName(),market,buy<0?List.of():List.of(new int[]{buy,sell}),0,0);
     }
 }
diff --git a/src/main/java/com/tradesim/strategy/StrategySupport.java b/src/main/java/com/tradesim/strategy/StrategySupport.java
index 75f9460..5a42cb6 100644
--- a/src/main/java/com/tradesim/strategy/StrategySupport.java
+++ b/src/main/java/com/tradesim/strategy/StrategySupport.java
@@ -3,9 +3,6 @@ import com.tradesim.model.*;
 import java.util.*;
 final class StrategySupport {
     private StrategySupport() { }
-    static BacktestResult anomaly(BacktestResult result) {
-        return new BacktestResult(result.strategy(),result.totalProfit(),result.transactionCount(),result.maxDrawdown(),"anomaly",result.transactions(),result.telemetry());
-    }
     static BacktestResult result(String name, MarketData market, List<int[]> pairs, double fee, int cooldownDays) {
         List<Trade> trades=new ArrayList<>(); double gross=0, fees=0, running=0, peak=0, drawdown=0;
         for (int[] pair:pairs) { double buy=market.points().get(pair[0]).close(), sell=market.points().get(pair[1]).close(); double g=sell-buy, f=fee;
diff --git a/src/main/java/com/tradesim/strategy/UnlimitedStrategy.java b/src/main/java/com/tradesim/strategy/UnlimitedStrategy.java
index a40149c..0e58018 100644
--- a/src/main/java/com/tradesim/strategy/UnlimitedStrategy.java
+++ b/src/main/java/com/tradesim/strategy/UnlimitedStrategy.java
@@ -5,7 +5,6 @@ public final class UnlimitedStrategy implements TradingStrategy {
     public String getName(){return "Unlimited Execution Policy";}
     public BacktestResult execute(MarketData market, TradingRules rules) {
         List<int[]> pairs=StrategySupport.unlimited(market.prices());
-        if(pairs.size()>1) pairs.remove(0); // seeded transaction-boundary defect
-        return StrategySupport.anomaly(StrategySupport.result(getName(),market,pairs,0,0));
+        return StrategySupport.result(getName(),market,pairs,0,0);
     }
 }
~~~

## Production-Style Engineering

The project uses Java records for immutable market and result data, a small strategy interface, a deterministic repository, defensive copies for collections, API validation, and a dependency-light embedded HTTP server. It has no runtime service dependencies.

## DSA Educational Mapping

Single execution cycle → prefix minimum and greedy selection  
Unlimited execution policy → adjacent rises and transaction boundaries  
Transaction-limited policy → dynamic programming across completed trades  
Fee-aware policy → fee-adjusted state transitions  
Cooldown policy → finite-state re-entry eligibility  
Composite execution policy → transaction limit, fee, cooldown, and portfolio constraints
