package com.tradesim.strategy;
import com.tradesim.model.*;
import java.util.*;
public final class CombinedStrategy implements TradingStrategy {
    public String getName(){return "Composite Execution Policy";}
    public BacktestResult execute(MarketData market, TradingRules rules) {
        List<Double> p=market.prices(); int n=p.size(), k=Math.min(rules.maxTransactions(),n/2);
        double[][] best=new double[k+1][n+1]; @SuppressWarnings("unchecked") List<int[]>[][] paths=(List<int[]>[][])new List<?>[k+1][n+1];
        for(int t=0;t<=k;t++)for(int d=0;d<=n;d++)paths[t][d]=new ArrayList<>();
        for(int t=1;t<=k;t++)for(int d=1;d<=n;d++){
            best[t][d]=best[t][d-1]; paths[t][d]=new ArrayList<>(paths[t][d-1]);
            for(int b=0;b<d-1;b++){int sell=d-1, prior=Math.max(0,b-rules.cooldownDays()-1);double value=best[t-1][prior]+p.get(sell)-p.get(b)-rules.transactionFee();
                if(value>best[t][d]){best[t][d]=value;paths[t][d]=new ArrayList<>(paths[t-1][prior]);paths[t][d].add(new int[]{b,sell});}}
        }
        return StrategySupport.result(getName(),market,paths[k][n],rules.transactionFee(),rules.cooldownDays());
    }
}
