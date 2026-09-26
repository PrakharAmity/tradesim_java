package com.tradesim.strategy;
import com.tradesim.model.*;
import java.util.*;
final class StrategySupport {
    private StrategySupport() { }
    static BacktestResult anomaly(BacktestResult result) {
        return new BacktestResult(result.strategy(),result.totalProfit(),result.transactionCount(),result.maxDrawdown(),"anomaly",result.transactions(),result.telemetry());
    }
    static BacktestResult result(String name, MarketData market, List<int[]> pairs, double fee, int cooldownDays) {
        List<Trade> trades=new ArrayList<>(); double gross=0, fees=0, running=0, peak=0, drawdown=0;
        for (int[] pair:pairs) { double buy=market.points().get(pair[0]).close(), sell=market.points().get(pair[1]).close(); double g=sell-buy, f=fee;
            trades.add(new Trade("BUY",pair[0]+1,buy,0,0)); trades.add(new Trade("SELL",pair[1]+1,sell,f,g-f)); gross+=g; fees+=f; running+=g-f; peak=Math.max(peak,running); drawdown=Math.max(drawdown,peak-running); }
        double net=gross-fees; int buys=pairs.size(), cooldownObserved=0;
        for(int i=1;i<pairs.size();i++) cooldownObserved+=Math.max(0,pairs.get(i)[0]-pairs.get(i-1)[1]-1);
        double finalValue=100_000+net;
        return new BacktestResult(name,net,pairs.size(),drawdown,"valid",trades,
                new BacktestResult.Telemetry(market.points().size(),buys,buys,fees,cooldownObserved,finalValue,finalValue,gross,net));
    }
    static List<int[]> unlimited(List<Double> p) {
        List<int[]> out=new ArrayList<>(); int n=p.size(), i=0;
        while(i<n-1){ while(i<n-1 && p.get(i+1)<=p.get(i)) i++; int buy=i;
            while(i<n-1 && p.get(i+1)>p.get(i)) i++; if(i>buy) out.add(new int[]{buy,i}); }
        return out;
    }
}
