package com.tradesim.model;
public record TradingRules(int maxTransactions, double transactionFee, int cooldownDays) {
    public TradingRules() { this(2, 2.0, 1); }
    public TradingRules {
        if (maxTransactions < 0) throw new IllegalArgumentException("maxTransactions must be non-negative");
        if (!Double.isFinite(transactionFee) || transactionFee < 0) throw new IllegalArgumentException("transactionFee must be non-negative");
        if (cooldownDays < 0) throw new IllegalArgumentException("cooldownDays must be non-negative");
    }
}
