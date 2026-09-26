package com.tradesim;
import com.tradesim.model.*;
import com.tradesim.strategy.CooldownStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CooldownStrategyTest {
 @Test void test_cooldown_reenter_validity(){var r=new CooldownStrategy().execute(SingleTradeStrategyTest.market(1,5,2,8,3,10),new TradingRules(5,0,1));int previousSell=-100;for(Trade t:r.transactions()){if(t.action().equals("BUY"))assertTrue(t.day()>=previousSell+2,"Cooldown violation: BUY on day "+t.day()+" after SELL on day "+previousSell);else previousSell=t.day();}assertEquals(2,new CooldownStrategy().execute(SingleTradeStrategyTest.market(1,4,2,5),new TradingRules(5,0,0)).transactionCount());assertEquals(1,new CooldownStrategy().execute(SingleTradeStrategyTest.market(1,4,2,5),new TradingRules(5,0,1)).transactionCount());assertEquals(0,new CooldownStrategy().execute(SingleTradeStrategyTest.market(9,7,5),new TradingRules(5,0,1)).transactionCount());}
}
