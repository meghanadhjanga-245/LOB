# ⚡ High-Frequency Limit Order Book (LOB) Simulator & Algorithmic Execution Terminal

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0-3178C6.svg)](https://www.typescriptlang.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg)](https://www.docker.com/)
[![CI](https://github.com/meghanadhjanga-245/LOB/actions/workflows/ci.yml/badge.svg)](https://github.com/meghanadhjanga-245/LOB/actions/workflows/ci.yml)

A deterministic, high-throughput Limit Order Book (LOB) exchange engine and institutional trading terminal built for quantitative microstructure research and algorithmic execution.

---

## 🌟 Live Demo & Deployment

| Resource | Link |
| :--- | :--- |
| **Live Interactive Web Terminal** | `https://lob-simulator.onrender.com` *(Replace with your live URL)* |
| **Source Repository** | [github.com/meghanadhjanga-245/LOB](https://github.com/meghanadhjanga-245/LOB) |
| **REST Health Endpoint** | `https://lob-simulator.onrender.com/actuator/health` |

---

## 🏛️ System Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        React / TypeScript Web UI                       │
│  ┌───────────────────┐  ┌─────────────────────┐  ┌──────────────────┐  │
│  │ TradingView Chart │  │    L2 Order Book    │  │  Algo Execution  │  │
│  │  (Candles + Vol)  │  │  (Depth Grouping)   │  │ (TWAP/Crash Lab) │  │
│  └───────────────────┘  └─────────────────────┘  └──────────────────┘  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTP / WebSocket (STOMP)
┌───────────────────────────────────▼────────────────────────────────────┐
│                  Spring Boot 3.2 Unified Microservice                  │
│     (Serves embedded SPA static assets + REST Endpoints on :8080)      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Low-latency Inter-thread Call
┌───────────────────────────────────▼────────────────────────────────────┐
│                    Deterministic LOB Matching Engine                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌───────────────────────┐ │
│  │  LMAX Disruptor  │  │ Fastutil Memory  │  │ Invariant Enforcement │ │
│  │  RingBuffer Queue│  │ RBTree Price Map │  │ (No Crossed Books)    │ │
│  └──────────────────┘  └──────────────────┘  └───────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Key Features

### 1. High-Performance Deterministic Engine
* **Zero Floating-Point Drift**: All prices represented as fixed-point integer ticks ($1 tick = $0.01).
* **Price-Time Priority (FIFO)**: Level-2 order queues optimized with `fastutil` primitive collections to minimize GC pauses.
* **Order Types**: Native support for `LIMIT`, `MARKET`, `IOC` (Immediate-Or-Cancel), and `ICEBERG` (hidden pool with peak size refill).
* **Real-Time Economics**: Tracks maker fee rebates, taker fees paid, continuous volume, and execution VWAP.

### 2. Quantitative Algorithmic Execution Lab
* **TWAP Benchmark Execution**: Slices target volume into discrete child market orders, recording benchmark arrival price, execution VWAP, and **Implementation Shortfall (in basis points)**.
* **Almgren-Chriss $\sqrt{Q}$ Power Law**: Evaluates non-linear market impact of aggressive sweeps vs. passive liquidity.
* **Flash Crash Stress Testing**: Injects severe sell cascades to simulate liquidity shocks, measuring spread blowout and resilience.
* **Avellaneda-Stoikov Market Making**: Automated quoting engine that shifts bid/ask reservations based on inventory risk.

### 3. Institutional Bloomberg/Binance Dark Terminal
* **TradingView Candlestick Engine**: Live 1s / 5s / 15s candlestick chart with volume histogram powered by `lightweight-charts`.
* **Active Working Orders Drawer**: Real-time table of user resting orders with 1-click cancellation and "Cancel All".
* **L2 Depth Grouping**: Aggregates adjacent ticks into buckets (`0.01` to `1.00`).
* **Web Audio Synthesis**: Low-latency synthesized sound cues for executions and institutional whale orders ($\ge 100$ units).

---

## 🛠️ Local Development & Quick Start

### 1. Clone & Build
```bash
git clone https://github.com/meghanadhjanga-245/LOB.git
cd LOB
mvn clean install -DskipTests
```

### 2. Run Single Unified Server (Port 8080)
The React dashboard is pre-compiled into Spring Boot:
```bash
mvn -pl service spring-boot:run
```
Open **http://localhost:8080** in your browser.

---

## 🐳 Docker Production Deployment

Run containerized locally:
```bash
docker compose up --build
```

---

## 📊 Endpoints Overview

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Web Trading Terminal UI |
| `GET` | `/api/orderbook/depth?levels=20` | Full L2 depth snapshot (Bids/Asks) |
| `POST` | `/api/orderbook` | Submit order (`LIMIT`, `MARKET`, `IOC`, `ICEBERG`) |
| `DELETE` | `/api/orderbook/{orderId}` | Cancel single resting order by ID |
| `DELETE` | `/api/orderbook` | Mass cancel all active orders (Clear book) |
| `GET` | `/api/orderbook/stats` | Spread, total volume, and invariant validation |
| `GET` | `/actuator/health` | Service health status |

---

## 📜 License
MIT License. Developed for quantitative finance, market microstructure analysis, and high-frequency trading research.
