package com.tradesim;
import com.tradesim.model.*;
import com.tradesim.strategy.SingleTradeStrategy;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class SingleTradeStrategyTest {
 @Test void test_single_trade_optimal(){
  MarketData m=market(11,8,5,9,12,6);
  var r=new SingleTradeStrategy().execute(m,new TradingRules());
  assertEquals(7,r.totalProfit(),1e-9); assertEquals(1,r.transactionCount());
  assertEquals(3,r.transactions().get(0).day()); assertEquals(5,r.transactions().get(0).price());
  assertEquals(5,new SingleTradeStrategy().execute(market(3,8),new TradingRules()).totalProfit(),1e-9);
  assertEquals(0,new SingleTradeStrategy().execute(market(9,7,5),new TradingRules()).totalProfit(),1e-9);
  assertEquals(0,new SingleTradeStrategy().execute(market(4),new TradingRules()).totalProfit(),1e-9);
  assertEquals(0,new SingleTradeStrategy().execute(market(4,4,4),new TradingRules()).totalProfit(),1e-9);
 }
 static MarketData market(double...p){var pts=new java.util.ArrayList<PricePoint>();for(int i=0;i<p.length;i++)pts.add(new PricePoint(i+1,"d"+(i+1),p[i]));return new MarketData("TEST",pts);}
}
