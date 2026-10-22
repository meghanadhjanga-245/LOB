# Using Real Market Data in LOB Simulator

The LOB Simulator now supports importing real market data to enhance experiments and analysis. While the core synthetic flow generator provides excellent control for experiments, real market data can be used for:

1. **Calibration**: Matching synthetic flow statistics to real market properties
2. **Validation**: Testing strategies against actual historical data
3. **Replay**: Executing strategies against real historical transactions
4. **Hybrid approaches**: Using real prices with synthetic order flow

## 📊 **Supported Data Formats**

### **1. OHLCV Data (Open, High, Low, Close, Volume)**
- **Use Case**: Generate synthetic orders around real price action
- **Sources**: Yahoo Finance, Alpha Vantage, Finnhub, Twelve Data, Google Finance
- **Frequency**: Any timeframe (1min, 5min, daily, etc.)
- **Format**: CSV with columns: `timestamp,open,high,low,close,volume` or `date,time,open,high,low,close,volume`

### **2. Trade Data (Time and Sales)**
- **Use Case**: Replay real transactions as market orders
- **Sources**: Historical trade data from exchanges (often available via paid services, but samples may be free)
- **Format**: CSV with columns: `timestamp,price,size,side` where side indicates buy/sell

### **3. Order Book Snapshots (Advanced)**
- **Use Case**: Direct reconstruction of historical market states
- **Note**: True L2 historical data is rare for free sources
- **Format**: Custom - see documentation for extending importers

## 🔧 **How to Use Real Market Data**

### **Option 1: OHLCV-Based Synthetic Flow (Recommended for most experiments)**
This approach uses real price history to drive realistic synthetic order generation:

```java
// In your experiment or test code:
import com.lobsimulator.flow.marketdata.MarketDataFactory;
import com.lobsimulator.flow.OrderFlowGenerator;

// Create importer for real OHLCV data
MarketDataImporter ohlcvImporter = MarketDataFactory.createOHLCVImporter(
    "path/to/your/data.csv",    // CSV file path
    12345L,                     // random seed
    50.0,                       // orders per bar (adjust based on volume)
    0.8,                        // use 80% of bar's high-low range
    0.5,                        // 50% buy orders
    0.1,                        // 10% market orders
    1,                          // min quantity
    100                         // max quantity
);

// Use the importer instead of or alongside synthetic flow
List<Order> realOrders = ohlcvImporter.importOrders();
```

### **Option 2: Direct Trade Replay**
Replay actual historical transactions:

```java
// For backtesting strategies against real transactions
MarketDataImporter tradeImporter = MarketDataFactory.createTradeImporter(
    "path/to/trades.csv",       // trade data CSV
    12345L,                     // seed
    1.0,                        // use actual trade sizes (1.0 = no scaling)
    0.0,                        // no additional delay between orders
    1,                          // min quantity
    10000                       // max quantity
);

List<Order> realTradeOrders = tradeImporter.importOrders();
```

### **Option 3: Hybrid Approach (Recommended)**
Use real prices for mid-point with synthetic depth:

```java
// 1. Get real-time or delayed quotes to set mid-price
// 2. Use synthetic flow generator around that mid-price
// 3. This combines real market levels with controllable order flow

// Example: Poll real quote API periodically to update base price
// Then use OrderFlowGenerator with that base price
```

## 📥 **Getting Free Market Data**

### **Free Data Sources (Requires Free Registration):**
1. **Alpha Vantage** (https://www.alphavantage.co/)
   - Free tier: 5 requests/minute, 500 requests/day
   - Supports: OHLCV, technical indicators, fundamentals
   - Get free API key after registration

2. **Finnhub** (https://finnhub.io/)
   - Free tier: 60 calls/minute
   - Supports: real-time quotes, fundamentals, forex, crypto
   - Get free API key after registration

3. **Twelve Data** (https://twelvedata.com/)
   - Free tier: 800 requests/day
   - Supports: OHLCV, real-time data, technical indicators
   - Get free API key after registration

4. **IEX Cloud** (https://iexcloud.io/)
   - Free tier: limited (requires payment for full access)
   - Supports: real-time and historical data

### **Completely Free Sources (No Registration):**
1. **Yahoo Finance** (unofficial, but widely used)
   - Note: No official API, but data can be scraped
   - Use with caution and respect rate limits
   - Many Python/Java libraries available

2. **Cryptocurrency Exchanges** (often free public APIs):
   - Binance, Coinbase, Kraken, etc.
   - No API key needed for public market data
   - Great for 24/7 markets with high volatility

3. **Exchange Sample Data**:
   - Many exchanges provide free sample datasets
   - NASDAQ, NYSE, CME groups often have sample data
   - Google: "[exchange] sample data CSV"

## 📁 **Data Format Examples**

### **OHLCV CSV Format:**
```
timestamp,open,high,low,close,volume
2023-01-02 09:30:00,150.25,151.80,149.90,151.20,1250000
2023-01-02 09:31:00,151.20,152.10,150.80,151.90,980000
2023-01-02 09:32:00,151.90,152.50,151.50,152.30,870000
```

### **Trade CSV Format:**
```
timestamp,price,size,side
2023-01-02 09:30:01.123,151.25,100,B
2023-01-02 09:30:01.456,151.20,200,S
2023-01-02 09:30:01.789,151.30,50,B
```

## ⚙️ **Integration Points**

### **With Existing Components:**
1. **Matching Engine**: Process imported orders directly
   ```java
   MatchingEngine engine = new MatchingEngine();
   List<TradeEvent> trades = engine.processOrder(realOrder);
   ```

2. **Order Book**: Use snapshots for experimental controls
   ```java
   OrderBook book = new OrderBook();
   // Process some orders to establish state
   // ...
   OrderBook snapshot = book.snapshot();
   // Run experiments, then restore:
   book.restore(snapshot);
   ```

3. **Market Impact Analysis**: Test strategies against real data
   ```java
   MarketImpactAnalyzer analyzer = new MarketImpactAnalyzer();
   // Use real orders to test impact algorithms
   ```

## 🧪 **Experiment Ideas with Real Data**

### **1. Strategy Calibration**
- Use real OHLCV data to calibrate synthetic flow parameters
- Match volatility, volume patterns, price movements
- Test if strategies perform similarly on synthetic vs real data

### **2. Historical Replay**
- Replay actual historical periods (flash crashes, Fed announcements, etc.)
- Test strategy robustness during volatile periods
- Compare performance across different market regimes

### **3. Parameter Sweep Automation**
- Automatically test strategy parameters across multiple days/weeks of real data
- Generate performance surfaces and robustness metrics
- Identify overfitting to specific market conditions

### **4. Hybrid Experiments**
- Use real prices from market data
- Generate synthetic order flow around those prices
- Combine realism of real levels with controllability of synthetic flow

## 📈 **Best Practices**

### **Data Quality:**
- Always validate data completeness and correctness
- Check for gaps, outliers, and anomalous values
- Adjust timestamps to consistent timezone (UTC recommended)

### **Performance:**
- Large datasets may require memory management
- Consider sampling or chunking for very large files
- Pre-process data when possible for repeated experiments

### **Reproducibility:**
- Record data source, date range, and any preprocessing steps
- Use fixed seeds for synthetic components
- Document exact experimental setup

### **Ethical Use:**
- Respect data source terms of service
- Do not redistribute proprietary data without permission
- Use data for educational/research purposes only unless licensed otherwise

## 🛠️ **Extending the System**

To add new data sources:
1. Implement the `MarketDataImporter` interface
2. Add a factory method in `MarketDataFactory`
3. Update documentation with usage examples
4. Consider adding to the composite importer for mixed-source experiments

## 💡 **Example Workflow**

1. **Download Data**: Get free OHLCV data from Alpha Vantage (with free API key)
2. **Prepare File**: Save as `AAPL_2023_01.csv` with columns: timestamp,open,high,low,close,volume
3. **Create Importer**:
   ```java
   MarketDataImporter importer = MarketDataFactory.createOHLCVImporter(
       "data/AAPL_2023_01.csv",
       2023L,           // seed matching the year
       75.0,            // orders per bar (adjust for average volume)
       0.85,            // use 85% of price range
       0.52,            // slight buy bias
       0.05,            // 5% market orders (mostly limit)
       1,               // min 1 share
       10000            // max 10k shares
   );
   ```
4. **Run Experiment**: Process orders through matching engine, analyze results
5. **Compare**: Run same experiment with pure synthetic flow for comparison

---

**Note**: While real market data adds authenticity, the synthetic flow generator remains valuable for:
- Controlled experiments (isolating variables)
- Reproducible research (fixed seeds)
- Scenario generation (what-if analysis)
- Performance benchmarking (known characteristics)

The combination of both approaches provides the most powerful experimental toolkit.