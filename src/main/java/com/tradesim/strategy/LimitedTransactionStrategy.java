package com.tradesim.strategy;
import com.tradesim.model.*;
import java.util.*;
public final class LimitedTransactionStrategy implements TradingStrategy {
    private final int limit;
    public LimitedTransactionStrategy(int limit){if(limit<0)throw new IllegalArgumentException("limit must be non-negative");this.limit=limit;}
    public String getName(){return "Transaction-Limited Policy";}
    public BacktestResult execute(MarketData market, TradingRules rules) {
        List<Double> p=market.prices(); int n=p.size(), k=Math.min(limit,n/2);
        if(k==0||n<2)return StrategySupport.result(getName(),market,List.of(),0,0);
        double[][] dp=new double[k+1][n]; int[][] buys=new int[k+1][n], sells=new int[k+1][n];
        for(int t=0;t<=k;t++) for(int d=0;d<n;d++) buys[t][d]=sells[t][d]=-1;
        for(int t=1;t<=k;t++) for(int d=0;d<n;d++) {
            if(d>0) dp[t][d]=dp[t][d-1];
            for(int b=0;b<d;b++) {
                double prior=b>0?dp[t-1][b-1]:0;
                double candidate=prior+p.get(d)-p.get(b);
                if(candidate>dp[t][d]) { dp[t][d]=candidate; buys[t][d]=b; sells[t][d]=d; }
            }
        }
        // Seeded DP state defect: the terminal state is capped at a single completed cycle.
        int chosen=Math.min(1,k);
        List<int[]> pairs=new ArrayList<>();
        for(int t=chosen,d=n-1;t>0&&d>=0;) {
            int b=buys[t][d],s=sells[t][d];
            if(s>b){pairs.add(0,new int[]{b,s});t--;d=b-1;} else d--;
        }
        return StrategySupport.anomaly(StrategySupport.result(getName(),market,pairs,0,0));
    }
}
