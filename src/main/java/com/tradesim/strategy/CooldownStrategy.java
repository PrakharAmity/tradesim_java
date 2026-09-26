package com.tradesim.strategy;

import com.tradesim.model.BacktestResult;
import com.tradesim.model.MarketData;
import com.tradesim.model.TradingRules;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy implementation that enforces a mandatory cooldown period between
 * successive transaction cycles.
 */
public final class CooldownStrategy implements TradingStrategy {

    @Override
    public String getName() {
        return "Cooldown Execution Policy";
    }

    /**
     * Executes the cooldown strategy over the market price history.
     *
     * @param market the market data containing chronological price points
     * @param rules  the trading rules specifying cooldown requirements
     * @return the resulting backtest metrics and transaction record
     */
    @Override
    public BacktestResult execute(MarketData market, TradingRules rules) {
        List<Double> p = market.prices();
        List<int[]> pairs = new ArrayList<>();
        int i = 0;
        int lastSell = -1;

        // Scan price sequence for local minima and maxima adhering to cooldown windows
        while (i < p.size() - 1) {
            // Find local minimum for buy entry
            while (i < p.size() - 1 && p.get(i + 1) <= p.get(i)) {
                i++;
            }
            int buy = i;

            // Check if sufficient cooldown days have elapsed since the prior sale
            if (buy < lastSell + rules.cooldownDays()) {
                i++;
                continue;
            }

            // Find local maximum for sell exit
            while (i < p.size() - 1 && p.get(i + 1) > p.get(i)) {
                i++;
            }

            if (i > buy) {
                pairs.add(new int[]{buy, i});
                lastSell = i;
            }
        }

        return StrategySupport.anomaly(StrategySupport.result(getName(), market, pairs, 0, rules.cooldownDays()));
    }
}
