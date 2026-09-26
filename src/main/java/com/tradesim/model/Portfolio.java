package com.tradesim.model;
import java.util.ArrayList;
import java.util.List;
/** Mutable state owned by one execution only. */
public final class Portfolio {
    private double cash, entryPrice, realizedProfit, feesPaid;
    private boolean holding;
    private int completedTransactions, cooldownRemaining;
    private final List<Trade> trades = new ArrayList<>();
    public void reset() { cash=entryPrice=realizedProfit=feesPaid=0; holding=false; completedTransactions=cooldownRemaining=0; trades.clear(); }
    public void buy(int day, double price) { if (holding) throw new IllegalStateException("Already holding"); holding=true; entryPrice=price; trades.add(new Trade("BUY",day,price,0,0)); }
    public void sell(int day, double price, double fee) { if (!holding) throw new IllegalStateException("No open position"); double gross=price-entryPrice, net=gross-fee; holding=false; completedTransactions++; feesPaid+=fee; realizedProfit+=net; trades.add(new Trade("SELL",day,price,fee,net)); }
    public double cash(){return cash;} public void setCash(double v){cash=v;} public boolean holding(){return holding;} public double entryPrice(){return entryPrice;}
    public int completedTransactions(){return completedTransactions;} public int cooldownRemaining(){return cooldownRemaining;} public void setCooldownRemaining(int v){cooldownRemaining=v;}
    public double getRealizedProfit(){return realizedProfit;} public List<Trade> getTrades(){return List.copyOf(trades);} public double getFeesPaid(){return feesPaid;}
}
