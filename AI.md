# TradeSim

## Description

TradeSim is a Java 17 financial market backtesting platform and execution policy simulation engine designed for quantitative developers and portfolio managers. It evaluates deterministic historical price series against algorithmic trading policies, calculating transaction ledgers, risk metrics, drawdowns, and operational telemetry. It runs as a lightweight HTTP service with an embedded HTTP server, an interactive browser dashboard, and an in-memory market data catalog.

## Repository Structure

```text
TradeSim Java/
├── pom.xml                 Maven build and project dependency configuration
├── challenge.json          Runtime environment, port, build, start, and test configuration
├── start.sh                Unix build, launch, and live-server startup script
├── start.ps1               PowerShell execution script for Windows server startup
├── start.bat               Windows batch script for server startup
├── README.md               Candidate-facing application documentation and bug guide
├── AI.md                   Challenge description, repository structure, and bug locations
├── src/
│   ├── main/
│   │   ├── java/com/tradesim/
│   │   │   ├── Main.java                        HTTP server entry point booting ApiServer on port 3000
│   │   │   ├── TestRunner.java                  Standalone regression test suite and JSON reporter
│   │   │   ├── data/
│   │   │   │   └── MarketDataRepository.java    In-memory deterministic price series (ACME_TECH, NOVA, STEEL)
│   │   │   ├── http/
│   │   │   │   └── ApiServer.java               Embedded HTTP server, REST endpoints, and static asset handler
│   │   │   ├── model/
│   │   │   │   ├── BacktestResult.java          Immutable backtest outcome record and Telemetry model
│   │   │   │   ├── MarketData.java              Market catalog representation and price points
│   │   │   │   ├── Portfolio.java               Stateful transaction tracker and execution ledger
│   │   │   │   ├── PricePoint.java              Timestamped price point representation
│   │   │   │   ├── Trade.java                   Individual BUY/SELL transaction record
│   │   │   │   └── TradingRules.java            Backtesting constraints (transaction limits, fees, cooldown)
│   │   │   ├── policy/
│   │   │   │   └── PolicyEngine.java            Strategy coordination, multi-policy comparisons, and state lifecycle
│   │   │   └── strategy/
│   │   │       ├── CombinedStrategy.java        Composite policy combining transaction limits, fees, and cooldowns
│   │   │       ├── CooldownStrategy.java        Execution policy enforcing mandatory idle cooldown between trades
│   │   │       ├── FeeStrategy.java             Fee-aware trading policy with fixed per-cycle fee deductions
│   │   │       ├── LimitedTransactionStrategy.java Dynamic programming policy bounding maximum transaction cycles
│   │   │       ├── SingleTradeStrategy.java     Optimal single buy-low/sell-high transaction cycle strategy
│   │   │       ├── StrategySupport.java         Helper utilities for fills, drawdowns, and anomaly wrapping
│   │   │       ├── TradingStrategy.java         Common strategy interface declaring execute()
│   │   │       └── UnlimitedStrategy.java       Multi-cycle trading policy capturing price movements
│   │   └── resources/
│   │       └── web/
│   │           ├── index.html                   Interactive dashboard structure and control panel
│   │           ├── style.css                    Dark-mode financial styling and responsive layout
│   │           └── app.js                       Frontend API dispatcher, telemetry metrics, and SVG chart renderer
│   └── test/
│       └── java/com/tradesim/
│           ├── CooldownStrategyTest.java        JUnit tests for cooldown policy execution and interval spacing
│           ├── FeeStrategyTest.java             JUnit tests for fee accounting and net profit deductions
│           ├── LimitedTransactionStrategyTest.java JUnit tests for transaction limits and DP path reconstruction
│           ├── PolicyEngineIsolationTest.java   JUnit tests for cross-run state isolation and reset semantics
│           ├── SingleTradeStrategyTest.java     JUnit tests for optimal single trade execution
│           └── UnlimitedStrategyTest.java       JUnit tests for multi-cycle profit capture
├── tests/
│   ├── run_tests.sh        Unix shell test rebuild and runner script
│   ├── run_tests.ps1       PowerShell test compilation and execution script
│   ├── run_tests.bat       Windows batch test execution script
│   └── run_tests.py        Python test invocation and verification script
└── scripts/
    └── seed_data.py        Deterministic market pricing generation and verification utility
```

## Bugs and Bug Locations

These are the six behavioral bug surfaces covered by the challenge. The named locations identify the owning implementation areas for debugging and review.

### 1. Single trade strategy produces zero profit

- **Bug location:** `src/main/java/com/tradesim/strategy/SingleTradeStrategy.java`, `SingleTradeStrategy::execute`
- **How to observe it:** Run `powershell .\tests\run_tests.ps1` (fails `test_single_trade_optimal`), or select `Single Execution Cycle` on the web interface with `ACME_TECH` or test sequence `market(11, 8, 5, 9, 12, 6)`.
- **Failure:** In the price iteration loop, `min` is overwritten with `p.get(i)` at every step (`min = p.get(i);`). Because `p.get(i) - min` evaluates to zero on every iteration, `best` is never updated, yielding $0.00 profit instead of the maximal buy-low/sell-high spread ($7.00 on test data, $60.00 on ACME_TECH). The result is also wrapped in `StrategySupport.anomaly`, surfacing an "Unexpected Result" badge.
- **Expected:** The strategy maintains a running minimum and its corresponding buy index up to the current day, finding the optimal single buy-low/sell-high pair (buying at $5.00 on Day 3 and selling at $12.00 on Day 5 for $7.00 profit on test data, or $60.00 on ACME_TECH), and returns a `"valid"` strategy status.

### 2. Unlimited trading drops the initial trade cycle

- **Bug location:** `src/main/java/com/tradesim/strategy/UnlimitedStrategy.java`, `UnlimitedStrategy::execute`
- **How to observe it:** Run `powershell .\tests\run_tests.ps1` (fails `test_unlimited_trades_profit`), or select `Unlimited Execution Policy` on the web interface with `ACME_TECH` or test sequence `market(1, 3, 2, 5, 4, 8)`.
- **Failure:** When multiple profitable intervals are detected, the implementation explicitly discards the first trade pair (`pairs.remove(0);`). On test sequence `market(1, 3, 2, 5, 4, 8)`, this reduces completed cycles from 3 to 2 and profit from $9.00 to $7.00. Furthermore, the result is wrapped in `StrategySupport.anomaly`.
- **Expected:** The unlimited trading policy preserves all profitable trade intervals detected across the price series (yielding 3 cycles and $9.00 on test data, or $135.00 on ACME_TECH), generates zero trades on declining markets (such as STEEL), and returns a `"valid"` strategy status.

### 3. Limited transaction strategy DP reconstruction hardcodes a single trade limit

- **Bug location:** `src/main/java/com/tradesim/strategy/LimitedTransactionStrategy.java`, `LimitedTransactionStrategy::execute`
- **How to observe it:** Run `powershell .\tests\run_tests.ps1` (fails `test_limited_transaction_dp`), or configure `maxTransactions` to 2 in the dashboard for `ACME_TECH` or test sequence `market(3, 10, 2, 8, 1, 5)`.
- **Failure:** Dynamic programming path reconstruction initializes the transaction target with `int chosen = Math.min(1, k);`, forcing backtracking to reconstruct at most 1 trade cycle even when `k = 2`. On test series `market(3, 10, 2, 8, 1, 5)`, profit is reported as $7.00 (1 trade) instead of $13.00 (2 trades). The result is also wrapped in `StrategySupport.anomaly`.
- **Expected:** Reconstruction starts from the full permitted cycle quota `k`, backtracking to collect up to `limit` non-overlapping optimal cycles (yielding $13.00 across 2 trades on test data, and $100.00 on ACME_TECH), respecting `transactionCount <= limit`, and returning a `"valid"` strategy status.

### 4. Fee-aware strategy double-deducts transaction fees

- **Bug location:** `src/main/java/com/tradesim/strategy/FeeStrategy.java`, `FeeStrategy::execute`
- **How to observe it:** Run `powershell .\tests\run_tests.ps1` (fails `test_fee_aware_strategy_profit`), or run the Fee-Aware Execution Policy with a $2.00 fee on `market(100, 110)` or in the dashboard.
- **Failure:** `StrategySupport.result()` already deducts the fee on each SELL fill. `FeeStrategy::execute` iterates through the generated trades and adds `rules.transactionFee()` a second time (`f += rules.transactionFee()`), doubling the fees deducted ($4.00 instead of $2.00) and understating net profit ($6.00 instead of $8.00). In addition, the status is hardcoded to `"anomaly"`.
- **Expected:** Each completed round-trip cycle deducts the flat transaction fee exactly once upon the sell fill. On `market(100, 110)` with a $2.00 fee, gross profit is $10.00, fees paid is $2.00, net profit is $8.00 ($127.00 on ACME_TECH), and the status is `"valid"`.

### 5. Cooldown strategy violates mandatory rest period on re-entry

- **Bug location:** `src/main/java/com/tradesim/strategy/CooldownStrategy.java`, `CooldownStrategy::execute`
- **How to observe it:** Run `powershell .\tests\run_tests.ps1` (fails `test_cooldown_reenter_validity`), or evaluate the Cooldown Execution Policy with `cooldownDays = 1` on price sequence `market(1, 5, 2, 8, 3, 10)`.
- **Failure:** The re-entry condition `buy < lastSell + rules.cooldownDays()` uses strict inequality rather than ensuring the full cooldown window has elapsed. With a 1-day cooldown, re-entering on day `lastSell + 1` is improperly allowed because `lastSell + 1 < lastSell + 1` evaluates to `false`. The result is also wrapped in `StrategySupport.anomaly`.
- **Expected:** Re-entry is permitted only after the full cooldown period has elapsed (`buy <= lastSell + rules.cooldownDays()` is rejected; re-entry requires `buy > lastSell + rules.cooldownDays()`). On sequence `(1, 5, 2, 8, 3, 10)`, trades adhere strictly to the mandatory 1-day rest window between sell and buy actions, and the status is `"valid"`.

### 6. PolicyEngine leaks residual fee state across consecutive simulation runs

- **Bug location:** `src/main/java/com/tradesim/policy/PolicyEngine.java`, `PolicyEngine::runBacktest`
- **How to observe it:** Run `powershell .\tests\run_tests.ps1` (fails `test_policy_engine_state_isolation`), or execute backtests sequentially for `fee` or `combined` policies in the UI.
- **Failure:** `PolicyEngine` caches and accumulates fees in a persistent `executionLedger` map across runs. On subsequent runs, non-zero carried fees (`extra != 0`) are subtracted from net profit and final portfolio value while inflating `feesPaid`, causing non-deterministic results ($47.00 on run 1 dropping to $15.00 on run 3) and overriding the status to `"anomaly"`.
- **Expected:** Backtests execute in complete state isolation without residual fee carryover. Repeated invocations with identical parameters yield deterministic, identical profit and telemetry, and return a `"valid"` status.

## Expected Behaviour After Fixing All Bugs

- `SingleTradeStrategy` tracks the global historical low and achieves the optimal single-cycle spread ($7.00 on test data, $60.00 on ACME_TECH) with `"valid"` status.
- `UnlimitedStrategy` captures every profitable interval without dropping initial trades ($9.00 on test data, $135.00 on ACME_TECH) and safely stays in cash during declining markets.
- `LimitedTransactionStrategy` backtracks from the full transaction quota, executing up to $k$ cycles ($13.00 on test data, $100.00 on ACME_TECH for 2 cycles) without quota violations.
- `FeeStrategy` deducts the transaction fee exactly once per completed round-trip cycle, accurately reporting gross profit, fees paid, and net profit ($8.00 net on test data, $127.00 on ACME_TECH).
- `CooldownStrategy` strictly enforces the mandatory rest period between a sell fill and the next buy entry (re-entry day $\ge \text{lastSell} + \text{cooldownDays} + 1$).
- `PolicyEngine` executes all strategy backtests statelessly and deterministically without cross-run fee leakage or telemetry corruption.
- Declining bear markets (such as STEEL) generate zero transactions and $0.00 profit across all strategies without entering loss-making positions.
- All strategies report status `"valid"`, clearing the UI anomaly banner to display `✓ Backtesting Engine Healthy`.
- Running `./tests/run_tests.sh` (or `powershell .\tests\run_tests.ps1`) passes all 6 tests with `"Passed": 6`, `"Failed": 0`, and exits with code 0.
