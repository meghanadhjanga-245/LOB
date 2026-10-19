# Limit Order Book Simulator & Market Impact Lab

## Project Overview
Build an exchange-style limit order book with a matching engine, feed it synthetic order flow calibrated to a real stock, and then place hypothetical market orders to measure execution costs. The result is a market-impact curve showing that execution cost grows faster than order size. A real-time API and dashboard demonstrate systems design, testing, and quantitative skills.

## Goals and Success Criteria

### Correctness
- Strict price-time priority
- No crossed book
- Quantity conservation
- Identical replay results

### Performance
- Target 500K+ orders/sec on a laptop

### Realism
- Spread, depth, and trade statistics within 15% of real stock data

### Insight
- Impact curve
- TWAP vs single order comparison

### Presentation
- Live dashboard
- Architecture diagram
- Design write-up

## System Architecture
Engine is a single-threaded deterministic core. Other services communicate via events.

### Components
- React + TypeScript Dashboard
- Spring Boot API Layer
- Order Flow Generator
- Matching Engine
- Experiment Runner
- Event Log & Replay (PostgreSQL Database)

## Technology Stack
- Core Engine: Java 21
- Collections: TreeMap, HashMap
- Backend: Spring Boot 3 + WebSocket
- Database: PostgreSQL + Flyway
- Frontend: React + TypeScript + Vite
- Charts: lightweight-charts / Recharts
- Testing: JUnit 5, jqwik, Testcontainers, JMH
- Analysis: Python (pandas, scipy, matplotlib)
- DevOps: GitHub Actions, Docker Compose, JaCoCo

## Roadmap Overview
1. Phase 0: Scope & Setup
2. Phase 1: Order Book
3. Phase 2: Matching Engine
4. Phase 3: Synthetic Flow
5. Phase 4: Market Impact
6. Phase 5: Backend API
7. Phase 6: Dashboard
8. Phase 7: Performance & Hardening
9. Phase 8: Documentation & Deployment

## Assumptions
- We will use integer ticks for prices to avoid floating-point inaccuracies.
- The matching engine is single-threaded for determinism.
- Order flow generation uses Poisson arrivals and seeded randomness for reproducibility.
- We will calibrate to a specific stock (e.g., AAPL) using historical data.
- The project will be open source and hosted on GitHub.
