package com.tradesim.data;
import com.tradesim.model.*;
import java.util.*;
public final class MarketDataRepository {
    private static final Map<String,List<Double>> SERIES=Map.of(
        "ACME_TECH",List.of(100d,103d,99d,108d,104d,116d,111d,107d,119d,114d,124d,120d,129d,121d,134d,128d,139d,131d),
        "NOVA",List.of(42d,39d,44d,48d,45d,53d,47d,55d,52d,61d,57d,65d,60d,68d,63d,72d),
        "STEEL",List.of(88d,86d,84d,83d,81d,82d,80d,79d,78d,78d,77d,76d,75d,76d,74d,73d));
    public List<String> tickers(){return List.of("ACME_TECH","NOVA","STEEL");}
    public MarketData get(String ticker){
        List<Double> values=SERIES.get(ticker); if(values==null)throw new IllegalArgumentException("Unknown ticker '"+ticker+"' is not available");
        List<PricePoint> points=new ArrayList<>(); for(int i=0;i<values.size();i++) points.add(new PricePoint(i+1,"2025-01-"+String.format("%02d",i+1),values.get(i)));
        return new MarketData(ticker,points);
    }
}
