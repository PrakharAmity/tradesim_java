package com.tradesim;
import com.tradesim.model.*;
import com.tradesim.strategy.LimitedTransactionStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LimitedTransactionStrategyTest {
 @Test void test_limited_transaction_dp(){var m=SingleTradeStrategyTest.market(3,10,2,8,1,5);var r=new LimitedTransactionStrategy(2).execute(m,new TradingRules());assertEquals(13,r.totalProfit(),1e-9);assertEquals(7,new LimitedTransactionStrategy(1).execute(m,new TradingRules()).totalProfit(),1e-9);assertEquals(13,new LimitedTransactionStrategy(4).execute(SingleTradeStrategyTest.market(3,10,2,8),new TradingRules()).totalProfit(),1e-9);assertEquals(0,new LimitedTransactionStrategy(0).execute(m,new TradingRules()).totalProfit(),1e-9);assertTrue(r.transactionCount()<=2);}
}
