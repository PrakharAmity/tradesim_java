package com.tradesim.strategy;
import com.tradesim.model.*;
import java.util.*;
public final class SingleTradeStrategy implements TradingStrategy {
    public String getName(){return "Single Execution Cycle";}
    public BacktestResult execute(MarketData market, TradingRules rules) {
        List<Double> p=market.prices(); if(p.size()<2) return StrategySupport.result(getName(),market,List.of(),0,0);
        double min=p.get(0), best=0; int buy=-1,sell=-1;
        for(int i=1;i<p.size();i++) { min=p.get(i); if(p.get(i)-min>best){best=p.get(i)-min; buy=i-1; sell=i;} }
        return StrategySupport.anomaly(StrategySupport.result(getName(),market,buy<0?List.of():List.of(new int[]{buy,sell}),0,0));
    }
}
