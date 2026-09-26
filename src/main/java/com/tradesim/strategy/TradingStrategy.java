package com.tradesim.strategy;
import com.tradesim.model.BacktestResult;
import com.tradesim.model.MarketData;
import com.tradesim.model.TradingRules;
public interface TradingStrategy {
    BacktestResult execute(MarketData marketData, TradingRules rules);
    String getName();
    default void reset() { }
}
