package com.tradesim.model;
public record Trade(String action, int day, double price, double fee, double realizedProfit) { }
