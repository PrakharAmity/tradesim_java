package com.tradesim.strategy;
import com.tradesim.model.*;
import java.util.*;
public final class UnlimitedStrategy implements TradingStrategy {
    public String getName(){return "Unlimited Execution Policy";}
    public BacktestResult execute(MarketData market, TradingRules rules) {
        List<int[]> pairs=StrategySupport.unlimited(market.prices());
        if(pairs.size()>1) pairs.remove(0); // seeded transaction-boundary defect
        return StrategySupport.anomaly(StrategySupport.result(getName(),market,pairs,0,0));
    }
}
