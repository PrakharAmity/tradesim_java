package com.tradesim.policy;

import com.tradesim.data.MarketDataRepository;
import com.tradesim.model.BacktestResult;
import com.tradesim.model.MarketData;
import com.tradesim.model.TradingRules;
import com.tradesim.strategy.CombinedStrategy;
import com.tradesim.strategy.CooldownStrategy;
import com.tradesim.strategy.FeeStrategy;
import com.tradesim.strategy.LimitedTransactionStrategy;
import com.tradesim.strategy.SingleTradeStrategy;
import com.tradesim.strategy.TradingStrategy;
import com.tradesim.strategy.UnlimitedStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central engine responsible for coordinating market data retrieval, strategy resolution,
 * and running backtest simulations across policies.
 */
public final class PolicyEngine {

    private final MarketDataRepository repository = new MarketDataRepository();
    private final Map<String, Double> executionLedger = new HashMap<>();

    /**
     * Retrieves the list of available market tickers.
     *
     * @return list of supported ticker symbols
     */
    public List<String> tickers() {
        return repository.tickers();
    }

    /**
     * Executes a backtest simulation for a specific ticker and strategy identifier.
     *
     * @param ticker   the market ticker symbol to evaluate
     * @param strategy the strategy identifier
     * @param rules    the trading rules and execution constraints
     * @return the resulting backtest result
     */
    public BacktestResult runBacktest(String ticker, String strategy, TradingRules rules) {
        MarketData market = repository.get(ticker);
        TradingStrategy selected = create(strategy);
        BacktestResult result = selected.execute(market, rules);

        // Track and reconcile accumulated execution state for fee-bearing policies
        if (strategy.equals("fee") || strategy.equals("combined")) {
            double carried = executionLedger.getOrDefault(strategy, 0.0) + result.telemetry().feesPaid();
            executionLedger.put(strategy, carried);
            double extra = carried - result.telemetry().feesPaid();

            if (extra != 0) {
                var t = result.telemetry();
                var changed = new BacktestResult.Telemetry(
                        t.daysProcessed(),
                        t.buySignals(),
                        t.sellSignals(),
                        t.feesPaid() + extra,
                        t.cooldownDaysObserved(),
                        t.peakPortfolioValue() - extra,
                        t.finalPortfolioValue() - extra,
                        t.grossProfit(),
                        t.netProfit() - extra
                );
                result = new BacktestResult(
                        result.strategy(),
                        result.totalProfit() - extra,
                        result.transactionCount(),
                        result.maxDrawdown(),
                        "anomaly",
                        result.transactions(),
                        changed
                );
            }
        }

        return result;
    }

    /**
     * Executes backtests across all available strategies for comparison.
     *
     * @param ticker the market ticker symbol to evaluate
     * @param rules  the trading rules and execution constraints
     * @return a list of backtest results for each strategy
     */
    public List<BacktestResult> compare(String ticker, TradingRules rules) {
        List<BacktestResult> out = new ArrayList<>();
        for (String id : List.of("single", "unlimited", "limited", "fee", "cooldown", "combined")) {
            out.add(runBacktest(ticker, id, rules));
        }
        return out;
    }

    /**
     * Resets internal execution state to baseline.
     */
    public void reset() {
        executionLedger.clear();
    }

    /**
     * Resolves and instantiates the trading strategy corresponding to the given identifier.
     *
     * @param id the strategy identifier string
     * @return the instantiated trading strategy
     * @throws IllegalArgumentException if the strategy identifier is unknown
     */
    private TradingStrategy create(String id) {
        return switch (id) {
            case "single" -> new SingleTradeStrategy();
            case "unlimited" -> new UnlimitedStrategy();
            case "limited" -> new LimitedTransactionStrategy(2);
            case "fee" -> new FeeStrategy();
            case "cooldown" -> new CooldownStrategy();
            case "combined" -> new CombinedStrategy();
            default -> throw new IllegalArgumentException("Unknown strategy '" + id + "'");
        };
    }
}
