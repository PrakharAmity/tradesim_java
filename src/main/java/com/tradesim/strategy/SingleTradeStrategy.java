package com.tradesim.strategy;

import com.tradesim.model.BacktestResult;
import com.tradesim.model.MarketData;
import com.tradesim.model.TradingRules;

import java.util.List;

/**
 * Strategy implementation that evaluates a single transaction cycle (one buy and one sell)
 * across the historical price series.
 */
public final class SingleTradeStrategy implements TradingStrategy {

    @Override
    public String getName() {
        return "Single Execution Cycle";
    }

    /**
     * Executes the single trade strategy across the provided market data.
     *
     * @param market the market data containing chronological price points
     * @param rules  the trading rules governing execution constraints
     * @return the resulting backtest metrics and transaction record
     */
    @Override
    public BacktestResult execute(MarketData market, TradingRules rules) {
        List<Double> p = market.prices();

        // Require at least two price points to form a buy-sell cycle
        if (p.size() < 2) {
            return StrategySupport.result(getName(), market, List.of(), 0, 0);
        }

        double min = p.get(0);
        double best = 0;
        int buy = -1;
        int sell = -1;

        // Iterate through the price series to identify optimal transaction indices
        for (int i = 1; i < p.size(); i++) {
            min = p.get(i);
            if (p.get(i) - min > best) {
                best = p.get(i) - min;
                buy = i - 1;
                sell = i;
            }
        }

        // Construct the transaction pair list and build the final backtest result
        List<int[]> pairs = (buy < 0) ? List.of() : List.of(new int[]{buy, sell});
        return StrategySupport.anomaly(StrategySupport.result(getName(), market, pairs, 0, 0));
    }
}
