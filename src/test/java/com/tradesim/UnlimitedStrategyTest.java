package com.tradesim;
import com.tradesim.model.*;
import com.tradesim.strategy.UnlimitedStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class UnlimitedStrategyTest {
 @Test void test_unlimited_trades_profit(){var r=new UnlimitedStrategy().execute(SingleTradeStrategyTest.market(1,3,2,5,4,8),new TradingRules());assertEquals(9,r.totalProfit(),1e-9);assertEquals(3,r.transactionCount());for(int i=0;i<r.transactions().size();i+=2){assertEquals("BUY",r.transactions().get(i).action());assertEquals("SELL",r.transactions().get(i+1).action());assertTrue(r.transactions().get(i).day()<r.transactions().get(i+1).day());}assertEquals(0,new UnlimitedStrategy().execute(SingleTradeStrategyTest.market(9,7,4),new TradingRules()).totalProfit(),1e-9);assertEquals(1,new UnlimitedStrategy().execute(SingleTradeStrategyTest.market(1,2,3,4),new TradingRules()).transactionCount());}
}
