# LOB Simulator

Limit Order Book Simulator & Market Impact Lab

See [SPEC.md](SPEC.md) for details.

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- Node.js 18+ (for frontend development)
- (Optional) Docker and Docker Compose

### Building the Project

To build all modules:

```bash
mvn clean install
```

To build a specific module:

```bash
mvn clean install -pl <module-name>
```

For example, to build only the engine module:
```bash
mvn clean install -pl engine
```

### Running the Application (Unified Single Link)

The frontend React dashboard is embedded directly into the Spring Boot service, serving both the UI and backend APIs together under a single URL.

```bash
cd service
mvn spring-boot:run
```

Open your browser at: **http://localhost:8080**

- **UI Dashboard**: http://localhost:8080/
- **REST API**: http://localhost:8080/api/orderbook
- **WebSocket STOMP**: http://localhost:8080/ws
- **Actuator Health**: http://localhost:8080/actuator/health

### (Optional) Frontend Development with Vite Dev Server

If you are developing frontend components with Hot Module Replacement (HMR):

```bash
cd web
npm install
npm run dev
```

The Vite dev server runs at http://localhost:3000 and automatically proxies `/api` and `/ws` requests to the backend at http://localhost:8080.

### Running Benchmarks

To run the performance benchmarks:

```bash
cd benchmarks
mvn clean install
java -jar target/benchmarks.jar
```

### Running Tests

To run unit tests for all modules:

```bash
mvn test
```

To run tests for a specific module:

```bash
mvn test -pl <module-name>
```

### Module Overview

1. **engine** - Core limit order book implementation (OrderBook, MatchingEngine, Order, PriceLevel, TradeEvent)
2. **flow** - Synthetic order flow generator with Poisson arrivals
3. **experiments** - Market impact analysis and experimental tools
4. **service** - Backend API layer (Spring Boot) with REST endpoints and WebSocket
5. **web** - Frontend dashboard (React + TypeScript + Vite)
6. **benchmarks** - Performance benchmarks using JMH
7. **notebooks** - Jupyter notebooks for analysis and experimentation

### Architecture

The system follows a modular architecture:

```
┌─────────────┐    ┌──────────────┐    ┌────────────────┐
│  Order Flow │───▶│  Matching    │───▶│  Order Book    │
│ Generator   │    │  Engine      │    │  (Core)        │
└─────────────┘    └──────────────┘    └────────────────┘
      ▲                   │                   │
      │                   ▼                   ▼
┌─────────────┐    ┌──────────────┐    ┌────────────────┐
│             │◀───│              │◀───│                │
│ Experiments │    │  Backend API │    │  Frontend      │
│ (Impact)    │    │  (Service)   │    │  Dashboard     │
└─────────────┘    └──────────────┘    └────────────────┘
```

### API Endpoints

The backend service provides the following REST endpoints:

- `POST /api/orderbook` - Submit a new order
- `DELETE /api/orderbook/{orderId}` - Cancel an order
- `GET /api/orderbook/best-bid` - Get best bid price and quantity
- `GET /api/orderbook/best-ask` - Get best ask price and quantity
- `GET /api/orderbook/spread` - Get current spread
- `GET /api/orderbook/is-crossed` - Check if book is crossed
- `GET /api/orderbook/check-invariants` - Validate internal invariants

WebSocket endpoints (via STOMP over SockJS at `/ws`):
- `/topic/orderbookUpdates` - General order book updates
- `/topic/bestPrices` - Best bid and ask updates
- `/topic/spread` - Spread updates

### Docker Compose

A basic Docker Compose setup is provided in `docker-compose.yml` for running the full stack.

```bash
docker-compose up
```

### Performance Targets

The system is designed to achieve:
- 500K+ orders/sec processing throughput on a modern laptop
- Sub-microsecond latency for order processing
- Deterministic behavior for replayability
- Real-time market impact analysis

### License

This project is open source and available under the MIT License.