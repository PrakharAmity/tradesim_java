package com.tradesim.strategy;

import com.tradesim.model.BacktestResult;
import com.tradesim.model.MarketData;
import com.tradesim.model.TradingRules;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy implementation that limits execution to a maximum number of completed
 * transaction cycles, utilizing dynamic programming across the price series.
 */
public final class LimitedTransactionStrategy implements TradingStrategy {

    private final int limit;

    /**
     * Constructs a limited transaction strategy with the specified cycle quota.
     *
     * @param limit the maximum number of allowed transaction cycles
     */
    public LimitedTransactionStrategy(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be non-negative");
        }
        this.limit = limit;
    }

    @Override
    public String getName() {
        return "Transaction-Limited Policy";
    }

    /**
     * Executes the transaction-limited strategy using dynamic programming.
     *
     * @param market the market data containing chronological price points
     * @param rules  the trading rules governing execution constraints
     * @return the resulting backtest metrics and transaction record
     */
    @Override
    public BacktestResult execute(MarketData market, TradingRules rules) {
        List<Double> p = market.prices();
        int n = p.size();
        int k = Math.min(limit, n / 2);

        // Return empty result if no transactions are permitted or insufficient data
        if (k == 0 || n < 2) {
            return StrategySupport.result(getName(), market, List.of(), 0, 0);
        }

        // Initialize DP table and tracking arrays for trade entry and exit points
        double[][] dp = new double[k + 1][n];
        int[][] buys = new int[k + 1][n];
        int[][] sells = new int[k + 1][n];

        for (int t = 0; t <= k; t++) {
            for (int d = 0; d < n; d++) {
                buys[t][d] = -1;
                sells[t][d] = -1;
            }
        }

        // Populate DP states: maximize profit across transaction counts and days
        for (int t = 1; t <= k; t++) {
            for (int d = 0; d < n; d++) {
                if (d > 0) {
                    dp[t][d] = dp[t][d - 1];
                }
                for (int b = 0; b < d; b++) {
                    double prior = (b > 0) ? dp[t - 1][b - 1] : 0;
                    double candidate = prior + p.get(d) - p.get(b);
                    if (candidate > dp[t][d]) {
                        dp[t][d] = candidate;
                        buys[t][d] = b;
                        sells[t][d] = d;
                    }
                }
            }
        }

        // Determine transaction cycle target for path reconstruction
        int chosen = Math.min(1, k);
        List<int[]> pairs = new ArrayList<>();

        // Reconstruct trade pairs by backtracking through the recorded decisions
        for (int t = chosen, d = n - 1; t > 0 && d >= 0;) {
            int b = buys[t][d];
            int s = sells[t][d];
            if (s > b) {
                pairs.add(0, new int[]{b, s});
                t--;
                d = b - 1;
            } else {
                d--;
            }
        }

        return StrategySupport.anomaly(StrategySupport.result(getName(), market, pairs, 0, 0));
    }
}
