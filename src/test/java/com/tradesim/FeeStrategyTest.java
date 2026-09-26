package com.tradesim;
import com.tradesim.model.*;
import com.tradesim.strategy.FeeStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FeeStrategyTest {
 @Test void test_fee_aware_strategy_profit(){var r=new FeeStrategy().execute(SingleTradeStrategyTest.market(100,110),new TradingRules(2,2,0));assertEquals(10,r.telemetry().grossProfit(),1e-9);assertEquals(2,r.telemetry().feesPaid(),1e-9);assertEquals(8,r.totalProfit(),1e-9);assertEquals(8,r.telemetry().netProfit(),1e-9);var multiple=new FeeStrategy().execute(SingleTradeStrategyTest.market(1,4,2,6),new TradingRules(4,1,0));assertEquals(2,multiple.transactionCount());assertEquals(2,multiple.telemetry().feesPaid(),1e-9);assertEquals(5,multiple.totalProfit(),1e-9);assertEquals(10,new FeeStrategy().execute(SingleTradeStrategyTest.market(100,110),new TradingRules(2,0,0)).totalProfit(),1e-9);assertEquals(0,new FeeStrategy().execute(SingleTradeStrategyTest.market(100,110),new TradingRules(2,10,0)).totalProfit(),1e-9);assertEquals(0,new FeeStrategy().execute(SingleTradeStrategyTest.market(100,110),new TradingRules(2,11,0)).transactionCount());}
}
