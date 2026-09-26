package com.tradesim.model;
import java.util.List;
public record BacktestResult(String strategy, double totalProfit, int transactionCount, double maxDrawdown,
                             String status, List<Trade> transactions, Telemetry telemetry) {
    public BacktestResult { transactions=List.copyOf(transactions); }
    public record Telemetry(int daysProcessed, int buySignals, int sellSignals, double feesPaid,
                            int cooldownDaysObserved, double peakPortfolioValue, double finalPortfolioValue,
                            double grossProfit, double netProfit) { }
}
