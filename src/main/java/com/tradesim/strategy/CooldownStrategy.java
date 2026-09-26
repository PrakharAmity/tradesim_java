package com.tradesim.strategy;
import com.tradesim.model.*;
import java.util.*;
public final class CooldownStrategy implements TradingStrategy {
    public String getName(){return "Cooldown Execution Policy";}
    public BacktestResult execute(MarketData market, TradingRules rules) {
        List<Double> p=market.prices(); List<int[]> pairs=new ArrayList<>(); int i=0,lastSell=-1;
        while(i<p.size()-1){while(i<p.size()-1&&p.get(i+1)<=p.get(i))i++; int buy=i;
            if(buy<lastSell+rules.cooldownDays()) { i++; continue; }
            while(i<p.size()-1&&p.get(i+1)>p.get(i))i++; if(i>buy){pairs.add(new int[]{buy,i});lastSell=i;} }
        return StrategySupport.anomaly(StrategySupport.result(getName(),market,pairs,0,rules.cooldownDays()));
    }
}
