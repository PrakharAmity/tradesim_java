package com.tradesim.strategy;

import com.tradesim.model.BacktestResult;
import com.tradesim.model.MarketData;
import com.tradesim.model.TradingRules;

import java.util.List;

/**
 * Strategy implementation that allows unlimited buy and sell transactions,
 * capturing price movements across sequential market intervals.
 */
public final class UnlimitedStrategy implements TradingStrategy {

    @Override
    public String getName() {
        return "Unlimited Execution Policy";
    }

    /**
     * Executes the unlimited transaction strategy on the provided market data.
     *
     * @param market the market data containing chronological price points
     * @param rules  the trading rules governing execution constraints
     * @return the resulting backtest metrics and transaction record
     */
    @Override
    public BacktestResult execute(MarketData market, TradingRules rules) {
        // Identify all profitable transaction intervals across the price series
        List<int[]> pairs = StrategySupport.unlimited(market.prices());

        // Adjust transaction pairs when multiple execution intervals are present
        if (pairs.size() > 1) {
            pairs.remove(0);
        }

        // Generate and format the backtest execution result
        return StrategySupport.anomaly(StrategySupport.result(getName(), market, pairs, 0, 0));
    }
}
