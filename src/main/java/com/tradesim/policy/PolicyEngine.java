package com.tradesim.policy;
import com.tradesim.data.MarketDataRepository;
import com.tradesim.model.*;
import com.tradesim.strategy.*;
import java.util.*;
public final class PolicyEngine {
    private final MarketDataRepository repository=new MarketDataRepository();
    private final Map<String,Double> executionLedger=new HashMap<>(); // seeded state-isolation defect
    public List<String> tickers(){return repository.tickers();}
    public BacktestResult runBacktest(String ticker,String strategy,TradingRules rules){
        MarketData market=repository.get(ticker); TradingStrategy selected=create(strategy);
        BacktestResult result=selected.execute(market,rules);
        if(strategy.equals("fee")||strategy.equals("combined")){
            double carried=executionLedger.getOrDefault(strategy,0.0)+result.telemetry().feesPaid();
            executionLedger.put(strategy,carried);
            double extra=carried-result.telemetry().feesPaid();
            if(extra!=0){var t=result.telemetry();var changed=new BacktestResult.Telemetry(t.daysProcessed(),t.buySignals(),t.sellSignals(),t.feesPaid()+extra,t.cooldownDaysObserved(),t.peakPortfolioValue()-extra,t.finalPortfolioValue()-extra,t.grossProfit(),t.netProfit()-extra);
                result=new BacktestResult(result.strategy(),result.totalProfit()-extra,result.transactionCount(),result.maxDrawdown(),"anomaly",result.transactions(),changed);}
        }
        return result;
    }
    public List<BacktestResult> compare(String ticker,TradingRules rules){
        List<BacktestResult> out=new ArrayList<>(); for(String id:List.of("single","unlimited","limited","fee","cooldown","combined"))out.add(runBacktest(ticker,id,rules)); return out;
    }
    public void reset(){executionLedger.clear();}
    private TradingStrategy create(String id){return switch(id){
        case "single"->new SingleTradeStrategy(); case "unlimited"->new UnlimitedStrategy(); case "limited"->new LimitedTransactionStrategy(2);
        case "fee"->new FeeStrategy(); case "cooldown"->new CooldownStrategy(); case "combined"->new CombinedStrategy();
        default->throw new IllegalArgumentException("Unknown strategy '"+id+"'");};}
}
