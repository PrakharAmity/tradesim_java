package com.tradesim;
import com.tradesim.model.TradingRules;
import com.tradesim.policy.PolicyEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PolicyEngineIsolationTest {
 @Test void test_policy_engine_state_isolation(){var e=new PolicyEngine();var rules=new TradingRules(2,2,1);var first=e.runBacktest("ACME_TECH","fee",rules);e.runBacktest("NOVA","cooldown",rules);var third=e.runBacktest("ACME_TECH","fee",rules);assertEquals(first.totalProfit(),third.totalProfit(),1e-9);assertEquals(first.transactionCount(),third.transactionCount());assertEquals(first.transactions(),third.transactions());assertEquals(first.telemetry(),third.telemetry());e.reset();assertEquals(first,e.runBacktest("ACME_TECH","fee",rules));assertNotEquals(e.runBacktest("NOVA","single",rules).totalProfit(),e.runBacktest("NOVA","cooldown",rules).totalProfit());}
}
