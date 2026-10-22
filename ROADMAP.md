# LOB Simulator: Path to Excellence Roadmap

This roadmap transforms the LOB Simulator from a research laboratory into a production-grade, high-performance exchange engine.

## 🚀 Phase 1: Trading Realism (Immediate)
*Goal: Implement industry-standard order behaviors.*

- [ ] **Advanced Order Types**:
    - [ ] Implement `Immediate-or-Cancel (IOC)`: Match as much as possible immediately, cancel the rest.
    - [ ] Implement `Fill-or-Kill (FOK)`: Match the entire quantity immediately or cancel the whole order.
    - [ ] Implement `Iceberg Orders`: Only expose a fraction of the total quantity to the book.
- [ ] **Market Order Logic**: Refine market orders to handle "slippage" more explicitly.

## ⚡ Phase 2: Low-Latency Engineering (High Impact)
*Goal: Move toward sub-microsecond processing.*

- [ ] **GC Pressure Reduction**:
    - [ ] Replace `java.util.TreeMap` in `OrderBook` with a primitive-based red-black tree or a fixed-size array of `PriceLevel` for the active range.
    - [ ] Use object pooling for `Order` and `TradeEvent` objects to avoid frequent allocation/collection.
- [ ] **Concurrency Model**:
    - [ ] Integrate **LMAX Disruptor** for the order ingestion pipeline.
    - [ ] Implement a single-threaded "pinned" matching loop to eliminate lock contention.
- [ ] **Data Structures**:
    - [ ] Move from `Integer` ticks to `long` for all price/quantity calculations to prevent any potential boxing.

## 🛠️ Phase 3: Infrastructure & Tooling (Professionalism)
*Goal: Ensure the system is observable and reproducible.*

- [ ] **Binary Event Store**:
    - [ ] Replace text-based logs with a binary format (e.g., SBE or Chronicle Queue) for ultra-fast replay.
- [ ] **CI/CD Performance Gates**:
    - [ ] Add a GitHub Action that runs `benchmarks` on every PR and fails if throughput drops by >5%.
- [ ] **Analysis Bridge**:
    - [ ] Create a `TradeExporter` that dumps execution logs directly to Apache Parquet for Python analysis.

## 📊 Phase 4: Visualization & Insight (Polish)
*Goal: Make the dashboard a professional trading terminal.*

- [ ] **Liquidity Heatmaps**:
    - [ ] Implement a historical depth map in the frontend (Price vs. Time vs. Volume).
- [ ] **P&L Analytics**:
    - [ ] Add real-time tracking of "Maker Profit" vs "Taker Cost."
- [ ] **Order Lifecycle Visualizer**:
    - [ ] Create a "Timeline View" for a single order from submission -> partial fill -> completion.
