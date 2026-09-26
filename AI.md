# AI Context

## Project

TradeSim is an in-memory financial market backtesting platform and execution policy simulator. It evaluates deterministic historical price series against algorithmic trading strategies, calculating transaction ledgers, risk metrics, drawdowns, and operational telemetry served via an embedded HTTP service and interactive web dashboard.

## Tech Stack

- **Language**: Java 17
- **Runtime**: Java SE Runtime Environment (embedded `com.sun.net.httpserver.HttpServer`)
- **Build / Packaging**: Maven 3.8+ (`pom.xml`), `javac`, `jar`
- **Testing**: JUnit Jupiter 5.10.2 (`org.junit.jupiter`), custom CLI `TestRunner`
- **Frontend**: Vanilla HTML5, CSS3, JavaScript (ES6+ fetch API, SVG rendering)

## Repository Structure

```text
src/
├── main/
│   ├── java/com/tradesim/
│   │   ├── data/       # Historical market data repository
│   │   ├── http/       # Embedded HTTP server and REST endpoints
│   │   ├── model/      # Domain records (MarketData, Trade, Rules, Portfolio)
│   │   ├── policy/     # Strategy resolution and backtest coordination
│   │   └── strategy/   # Algorithmic trading strategies and accounting support
│   └── resources/
│       └── web/        # Static dashboard frontend (HTML, CSS, JS)
└── test/
    └── java/com/tradesim/ # JUnit 5 unit test suites
tests/                  # Standalone test runner scripts (sh, ps1, bat, py)
scripts/                # Environment and challenge metadata
```

## Entry Points

- **Web Server Startup**: `com.tradesim.Main.main(String[] args)` — boots `ApiServer` on port 3000 (overridden by `PORT` env var).
- **CLI Test Suite**: `com.tradesim.TestRunner.main(String[] args)` — executes automated strategy test cases and emits structured JSON reporting to stdout.
- **REST Dispatcher**: `ApiServer.start(int port)` — maps contexts `/api/state`, `/api/backtest`, `/api/reset`, and static assets `/`.
- **Browser Client**: `src/main/resources/web/app.js:init()` — loads application state, configures UI dropdowns, and triggers scenario backtests.

## Architecture

```text
Browser Client (app.js)
  → HTTP POST /api/backtest (JSON payload)
  → ApiServer: validates method & extracts parameters
  → TradingRules (immutable record validation)
  → PolicyEngine: coordinates execution & accesses MarketDataRepository
  → TradingStrategy: executes policy algorithm against PricePoint series
  → Portfolio / StrategySupport: computes fills, P&L, fees, and telemetry
  → BacktestResult (immutable result record)
  → ApiServer: serializes to JSON response
  → Browser UI: renders SVG price chart, telemetry metrics, and transaction tables
```

## Important Modules

### `com.tradesim.http.ApiServer`
- Hosts REST endpoints (`/api/state`, `/api/backtest`, `/api/reset`) and serves static UI assets from classpath resources.
- Parses JSON request bodies and maps HTTP exceptions to structured error responses.
- Serializes `MarketData` and `BacktestResult` domain objects into JSON output.

### `com.tradesim.policy.PolicyEngine`
- Central coordinator between market data series and trading strategies.
- Resolves ticker data from `MarketDataRepository` and instantiates strategies by identifier (`single`, `unlimited`, `limited`, `fee`, `cooldown`, `combined`).
- Manages comparative multi-strategy runs via `compare(ticker, rules)`.

### `com.tradesim.strategy` (Trading Strategies)
- `TradingStrategy`: base interface declaring `execute(MarketData, TradingRules)`.
- Strategy implementations: `SingleTradeStrategy`, `UnlimitedStrategy`, `LimitedTransactionStrategy`, `FeeStrategy`, `CooldownStrategy`, and `CombinedStrategy`.
- `StrategySupport`: helper for multi-interval monotonic scanning, drawdown calculations, fee deductions, and `BacktestResult` construction.

### `com.tradesim.data.MarketDataRepository`
- In-memory store providing deterministic closing price sequences (`ACME_TECH`, `NOVA`, `STEEL`).
- Validates ticker availability and transforms raw price arrays into ordered `PricePoint` collections.

### `com.tradesim.model`
- Domain records: `MarketData`, `PricePoint`, `Trade`, `TradingRules`, and `BacktestResult` (with nested `Telemetry`).
- `Portfolio`: stateful trade execution tracker managing cash balance, entry price, open positions, fee accumulation, and completed cycles.

## Data Relationships

- Client Payload → `ApiServer.parse()` → parameter map (`ticker`, `strategy`, `maxTransactions`, `transactionFee`, `cooldownDays`).
- Parameter map → `TradingRules` record constructor (validates non-negative constraints).
- Ticker string → `MarketDataRepository.get()` → validates existence and produces `MarketData(ticker, List<PricePoint>)`.
- `MarketData` + `TradingRules` → `TradingStrategy.execute()` → algorithmic transaction signals (BUY/SELL).
- Transaction signals → `StrategySupport.result()` / `Portfolio` → builds `List<Trade>` and aggregates `Telemetry`.
- `BacktestResult` → `ApiServer.serialize()` → JSON HTTP 200 response.
- JSON response → `app.js:render()` → updates SVG line chart, telemetry indicators, and transaction tables.

## Debugging Invariants

- **Rules Validation**: `TradingRules` invariants enforce non-negative values for transaction limits, fees, and cooldown periods upon instantiation.
- **Ordered Signals**: Every executed trade cycle requires a `BUY` action strictly preceding its corresponding `SELL` action.
- **Time Monotonicity**: Transaction day indices are strictly positive and must be chronologically valid with respect to the input price sequence.
- **Accounting Derivation**: Net profit must equal gross profit minus total transaction fees paid.
- **Result Immutability**: `BacktestResult`, `Telemetry`, and `Trade` instances are immutable records once generated.
- **Execution Reproducibility**: Given an identical ticker, strategy, and rule set, repeated invocations must produce deterministic, reproducible fills and telemetry.

## Testing

- **Framework**: JUnit Jupiter 5 (`org.junit.jupiter`) and standalone CLI `TestRunner`.
- **Test Locations**:
  - `src/test/java/com/tradesim/`: JUnit test classes covering individual strategies and policy engine behavior.
  - `src/main/java/com/tradesim/TestRunner.java`: Command-line harness executing the primary strategy regression checks.
- **Test Runners**: `tests/run_tests.sh`, `tests/run_tests.ps1`, `tests/run_tests.bat`, and `tests/run_tests.py` execute `com.tradesim.TestRunner` and output standardized JSON diagnostics.

## Runtime / Commands

- **Start Web Application**: `powershell .\start.ps1` or `java -jar target/tradesim.jar` (runs server on `http://localhost:3000`).
- **Run Standalone Test Suite**: `powershell .\tests\run_tests.ps1` or `java -cp target/tradesim.jar com.tradesim.TestRunner`.
- **Run JUnit Tests via Maven**: `mvn test`.
- **Compile & Package JAR**: `mvn -q -DskipTests package`.

## Debugging Context

- **Boundary Separation**: HTTP request parsing and serialization occur solely in `ApiServer`; business calculations are confined to `strategy/` and `policy/`.
- **State Scope**: `Portfolio` encapsulates mutable state within a single simulation execution, whereas `BacktestResult` models immutable output.
- **Data Series Scope**: Available tickers are fixed in `MarketDataRepository`; requesting an unregistered ticker throws `IllegalArgumentException` mapped to HTTP 400.
- **Engine State**: Policy engine supports scenario state resets via `POST /api/reset` to prevent cross-run state pollution.
