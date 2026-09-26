package com.tradesim;

import com.tradesim.model.*;
import com.tradesim.policy.PolicyEngine;
import com.tradesim.strategy.*;
import java.util.*;

public final class TestRunner {

    private record TestResult(String name, String status, String execTime, String error) {}

    public static void main(String[] args) {
        List<String> names = List.of(
            "test_single_trade_optimal",
            "test_unlimited_trades_profit",
            "test_limited_transaction_dp",
            "test_fee_aware_strategy_profit",
            "test_cooldown_reenter_validity",
            "test_policy_engine_state_isolation"
        );

        Map<String, TestResult> results = new LinkedHashMap<>();
        int totalExecMs = 0;

        for (String name : names) {
            long start = System.currentTimeMillis();
            try {
                switch (name) {
                    case "test_single_trade_optimal" -> runSingleTrade();
                    case "test_unlimited_trades_profit" -> runUnlimited();
                    case "test_limited_transaction_dp" -> runLimited();
                    case "test_fee_aware_strategy_profit" -> runFee();
                    case "test_cooldown_reenter_validity" -> runCooldown();
                    case "test_policy_engine_state_isolation" -> runPolicyEngine();
                }
                long duration = System.currentTimeMillis() - start;
                totalExecMs += duration;
                results.put(name, new TestResult(name, "passed", duration + "ms", null));
            } catch (Throwable t) {
                long duration = System.currentTimeMillis() - start;
                totalExecMs += duration;
                String msg = t.getMessage();
                if (msg == null || msg.isBlank()) msg = t.getClass().getSimpleName();
                results.put(name, new TestResult(name, "failed", duration + "ms", msg));
            }
        }

        int passed = (int) results.values().stream().filter(r -> "passed".equals(r.status())).count();
        int failed = results.size() - passed;

        // Print JSON matching standard runner format
        StringBuilder json = new StringBuilder("{\n");
        for (String name : names) {
            TestResult r = results.get(name);
            json.append("  \"").append(name).append("\": {\n");
            json.append("    \"Status\": \"").append(r.status()).append("\",\n");
            json.append("    \"Execution time\": \"").append(r.execTime()).append("\"");
            if (r.error() != null) {
                json.append(",\n    \"Error\": \"").append(escape(r.error())).append("\"\n");
            } else {
                json.append("\n");
            }
            json.append("  },\n");
        }
        json.append("  \"Passed\": ").append(passed).append(",\n");
        json.append("  \"Failed\": ").append(failed).append(",\n");
        json.append("  \"Total bugs\": 6,\n");
        json.append("  \"Total Execution time\": \"").append(totalExecMs).append("ms\"\n");
        json.append("}");

        System.out.println(json.toString());
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void runSingleTrade() {
        MarketData m = market(11, 8, 5, 9, 12, 6);
        BacktestResult r = new SingleTradeStrategy().execute(m, new TradingRules());
        if (Math.abs(7 - r.totalProfit()) > 1e-9) {
            throw new AssertionError(String.format(Locale.US, "Single-trade report mismatch. Expected $7.000000, got $%.6f.", r.totalProfit()));
        }
        if (r.transactionCount() != 1) {
            throw new AssertionError(String.format("Expected 1 trade, got %d", r.transactionCount()));
        }
        if (r.transactions().get(0).day() != 3) {
            throw new AssertionError("Expected buy on Day 3");
        }
        if (Math.abs(5 - r.transactions().get(0).price()) > 1e-9) {
            throw new AssertionError("Expected buy price $5.00");
        }
        if (Math.abs(5 - new SingleTradeStrategy().execute(market(3, 8), new TradingRules()).totalProfit()) > 1e-9) {
            throw new AssertionError("Expected $5 profit for market(3, 8)");
        }
        if (Math.abs(0 - new SingleTradeStrategy().execute(market(9, 7, 5), new TradingRules()).totalProfit()) > 1e-9) {
            throw new AssertionError("Declining market should not produce a positive return.");
        }
    }

    private static void runUnlimited() {
        BacktestResult r = new UnlimitedStrategy().execute(market(1, 3, 2, 5, 4, 8), new TradingRules());
        if (Math.abs(9 - r.totalProfit()) > 1e-9) {
            throw new AssertionError(String.format(Locale.US, "Unlimited-trading report mismatch. Expected $9.000000, got $%.6f.", r.totalProfit()));
        }
        if (r.transactionCount() != 3) {
            throw new AssertionError(String.format("Expected 3 completed cycles, got %d", r.transactionCount()));
        }
        for (int i = 0; i < r.transactions().size(); i += 2) {
            if (!"BUY".equals(r.transactions().get(i).action())) throw new AssertionError("Expected BUY action");
            if (!"SELL".equals(r.transactions().get(i + 1).action())) throw new AssertionError("Expected SELL action");
            if (r.transactions().get(i).day() >= r.transactions().get(i + 1).day()) throw new AssertionError("BUY day must precede SELL day");
        }
        if (Math.abs(0 - new UnlimitedStrategy().execute(market(9, 7, 4), new TradingRules()).totalProfit()) > 1e-9) {
            throw new AssertionError("Declining market should produce zero return.");
        }
    }

    private static void runLimited() {
        MarketData m = market(3, 10, 2, 8, 1, 5);
        BacktestResult r = new LimitedTransactionStrategy(2).execute(m, new TradingRules());
        if (Math.abs(13 - r.totalProfit()) > 1e-9) {
            throw new AssertionError(String.format(Locale.US, "Two-cycle mandate report mismatch. Expected $13.000000, got $%.6f.", r.totalProfit()));
        }
        if (Math.abs(7 - new LimitedTransactionStrategy(1).execute(m, new TradingRules()).totalProfit()) > 1e-9) {
            throw new AssertionError("Single-cycle limit report mismatch.");
        }
        if (r.transactionCount() > 2) {
            throw new AssertionError("Reported cycles exceeded permitted limit 2");
        }
    }

    private static void runFee() {
        BacktestResult r = new FeeStrategy().execute(market(100, 110), new TradingRules(2, 2, 0));
        if (Math.abs(10 - r.telemetry().grossProfit()) > 1e-9) {
            throw new AssertionError(String.format(Locale.US, "Gross profit mismatch: expected $10.00, got $%.2f", r.telemetry().grossProfit()));
        }
        if (Math.abs(2 - r.telemetry().feesPaid()) > 1e-9) {
            throw new AssertionError(String.format(Locale.US, "Fee accounting mismatch: expected $2.00, got $%.2f", r.telemetry().feesPaid()));
        }
        if (Math.abs(8 - r.totalProfit()) > 1e-9) {
            throw new AssertionError(String.format(Locale.US, "Expected net profit $8 after one $2 fee per completed cycle. Expected $8.000000, got $%.6f.", r.totalProfit()));
        }
    }

    private static void runCooldown() {
        BacktestResult r = new CooldownStrategy().execute(market(1, 5, 2, 8, 3, 10), new TradingRules(5, 0, 1));
        int previousSell = -100;
        for (Trade t : r.transactions()) {
            if ("BUY".equals(t.action())) {
                if (t.day() < previousSell + 2) {
                    throw new AssertionError("Strategy violated 1-day mandatory cooldown after Day " + previousSell + ".");
                }
            } else {
                previousSell = t.day();
            }
        }
        if (new CooldownStrategy().execute(market(1, 4, 2, 5), new TradingRules(5, 0, 1)).transactionCount() != 1) {
            throw new AssertionError("Cooldown policy should only complete 1 trade on tight sequence.");
        }
    }

    private static void runPolicyEngine() {
        PolicyEngine e = new PolicyEngine();
        TradingRules rules = new TradingRules(2, 2, 1);
        BacktestResult first = e.runBacktest("ACME_TECH", "fee", rules);
        e.runBacktest("NOVA", "cooldown", rules);
        BacktestResult third = e.runBacktest("ACME_TECH", "fee", rules);

        if (Math.abs(first.totalProfit() - third.totalProfit()) > 1e-9) {
            throw new AssertionError(String.format(Locale.US, "PolicyEngine state leakage: first run profit $%.2f != third run profit $%.2f", first.totalProfit(), third.totalProfit()));
        }
        if (first.transactionCount() != third.transactionCount()) {
            throw new AssertionError("Transaction count mismatch due to state leakage");
        }
        if (!first.transactions().equals(third.transactions())) {
            throw new AssertionError("Transactions mismatch due to state leakage");
        }
        if (!first.telemetry().equals(third.telemetry())) {
            throw new AssertionError("Telemetry mismatch due to state leakage");
        }
        e.reset();
        if (!first.equals(e.runBacktest("ACME_TECH", "fee", rules))) {
            throw new AssertionError("Reset failed to restore state");
        }
    }

    private static MarketData market(double... p) {
        List<PricePoint> pts = new ArrayList<>();
        for (int i = 0; i < p.length; i++) {
            pts.add(new PricePoint(i + 1, "d" + (i + 1), p[i]));
        }
        return new MarketData("TEST", pts);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}
