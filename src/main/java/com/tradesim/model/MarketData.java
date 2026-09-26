package com.tradesim.model;
import java.util.List;
public record MarketData(String ticker, List<PricePoint> points) {
    public MarketData { points = List.copyOf(points); }
    public List<Double> prices() { return points.stream().map(PricePoint::close).toList(); }
}
