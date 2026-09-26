package com.tradesim.strategy;

import com.tradesim.model.BacktestResult;
import com.tradesim.model.MarketData;
import com.tradesim.model.Trade;
import com.tradesim.model.TradingRules;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy implementation that incorporates fixed per-transaction fee deductions
 * into the execution cycle evaluation and portfolio telemetry.
 */
public final class FeeStrategy implements TradingStrategy {

    @Override
    public String getName() {
        return "Fee-Aware Execution Policy";
    }

    /**
     * Executes the fee-aware strategy, calculating transaction pairs and adjusting ledgers.
     *
     * @param market the market data containing chronological price points
     * @param rules  the trading rules governing execution constraints and fees
     * @return the resulting backtest metrics and transaction record
     */
    @Override
    public BacktestResult execute(MarketData market, TradingRules rules) {
        List<int[]> pairs = feeAware(market.prices(), rules.transactionFee());
        BacktestResult once = StrategySupport.result(getName(), market, pairs, rules.transactionFee(), 0);

        List<Trade> trades = new ArrayList<>();
        double feeTotal = 0;

        // Process individual transaction records and adjust fee totals on sell fills
        for (Trade t : once.transactions()) {
            double f = t.fee();
            if (t.action().equals("SELL")) {
                f += rules.transactionFee();
                feeTotal += f;
                trades.add(new Trade(t.action(), t.day(), t.price(), f, t.realizedProfit() - rules.transactionFee()));
            } else {
                trades.add(t);
            }
        }

        // Calculate final net profit after accounting for transaction costs
        double net = once.totalProfit() - feeTotal + once.telemetry().feesPaid();

        var tele = new BacktestResult.Telemetry(
                once.telemetry().daysProcessed(),
                once.telemetry().buySignals(),
                once.telemetry().sellSignals(),
                feeTotal,
                0,
                100000 + net,
                100000 + net,
                once.telemetry().grossProfit(),
                net
        );

        return new BacktestResult(getName(), net, pairs.size(), once.maxDrawdown(), "anomaly", trades, tele);
    }

    /**
     * Internal state representation for fee-aware sequence optimization.
     */
    private record State(double value, List<int[]> pairs, int buyDay) { }

    /**
     * Finds profitable transaction pairs considering transaction fees via a forward state scan.
     *
     * @param prices list of historical prices
     * @param fee    per-transaction fee amount
     * @return list of buy-sell day index pairs
     */
    private List<int[]> feeAware(List<Double> prices, double fee) {
        if (prices.size() < 2) {
            return List.of();
        }

        // Initialize state tracking for cash and holding positions
        State cash = new State(0, List.of(), -1);
        State hold = new State(-prices.get(0), List.of(), 0);

        for (int day = 1; day < prices.size(); day++) {
            double close = prices.get(day);

            // Evaluate exiting position to cash
            State nextCash = cash;
            double realized = hold.value() + close - fee;
            if (realized > cash.value()) {
                List<int[]> completed = new ArrayList<>(hold.pairs());
                completed.add(new int[]{hold.buyDay(), day});
                nextCash = new State(realized, List.copyOf(completed), -1);
            }

            // Evaluate entering position from cash
            State nextHold = hold;
            double entry = cash.value() - close;
            if (entry > hold.value()) {
                nextHold = new State(entry, cash.pairs(), day);
            }

            cash = nextCash;
            hold = nextHold;
        }

        return cash.pairs();
    }
}
