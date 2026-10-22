package com.lobsimulator.service.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lobsimulator.engine.Order;
import com.lobsimulator.engine.OrderBook;
import com.lobsimulator.engine.MatchingEngine;
import com.lobsimulator.engine.OrderType;
import com.lobsimulator.engine.TradeEvent;
import com.lobsimulator.service.dto.OrderRequest;
import com.lobsimulator.service.dto.OrderResponse;
import com.lobsimulator.service.dto.TradeResponse;

@RestController
@RequestMapping("/api/orderbook")
@CrossOrigin(originPatterns = "*")
public class OrderBookController {

    private final OrderBook orderBook;
    private final MatchingEngine matchingEngine;

    public OrderBookController(OrderBook orderBook, MatchingEngine matchingEngine) {
        this.orderBook = orderBook;
        this.matchingEngine = matchingEngine;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> submitOrder(@RequestBody OrderRequest orderRequest) {
        OrderType type;
        String reqType = orderRequest.getOrderType() != null ? orderRequest.getOrderType().trim().toUpperCase() : null;
        if (reqType != null && !reqType.isEmpty()) {
            if ("ICEBERG".equals(reqType)) {
                type = OrderType.LIMIT;
            } else {
                try {
                    type = OrderType.valueOf(reqType);
                } catch (IllegalArgumentException e) {
                    type = orderRequest.isMarket() ? OrderType.MARKET : OrderType.LIMIT;
                }
            }
        } else {
            type = orderRequest.isMarket() ? OrderType.MARKET : OrderType.LIMIT;
        }

        Order order = new Order(
                orderRequest.getOrderId(),
                orderRequest.isBuySide(),
                orderRequest.getPrice(),
                orderRequest.getQuantity(),
                orderRequest.getTimestamp() > 0 ? orderRequest.getTimestamp() : System.nanoTime(),
                type
        );

        if ("ICEBERG".equalsIgnoreCase(reqType) || orderRequest.getPeakSize() > 0) {
            int peak = orderRequest.getPeakSize() > 0 ? orderRequest.getPeakSize() : Math.min(orderRequest.getQuantity(), 10);
            order.setIceberg(true, peak);
        }

        List<TradeEvent> events = matchingEngine.processOrder(order);

        List<TradeResponse> trades = events.stream().map(e -> new TradeResponse(
                e.getPrice(),
                e.getQty(),          // fixed: was e.getQuantity()
                e.isBuySide(),
                e.getMakerId(),      // fixed: was e.getMakerOrderId()
                e.getTakerId(),      // fixed: was e.getTakerOrderId()
                e.getTimestamp(),
                e.getMakerFee(),
                e.getTakerFee(),
                e.getSlippage(),
                e.getVwap()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(new OrderResponse(orderRequest.getOrderId(), trades));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Boolean> cancelOrder(@PathVariable("orderId") String orderId) {
        return ResponseEntity.ok(orderBook.cancelOrder(orderId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearBook() {
        orderBook.clear();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/best-bid")
    public ResponseEntity<long[]> getBestBid() {
        return ResponseEntity.ok(orderBook.getBestBid());
    }

    @GetMapping("/best-ask")
    public ResponseEntity<long[]> getBestAsk() {
        return ResponseEntity.ok(orderBook.getBestAsk());
    }

    @GetMapping("/spread")
    public ResponseEntity<Double> getSpread() {
        long[] bestBid = orderBook.getBestBid();
        long[] bestAsk = orderBook.getBestAsk();
        if (bestBid == null || bestAsk == null) return ResponseEntity.ok(0.0);
        return ResponseEntity.ok((double) (bestAsk[0] - bestBid[0]));
    }

    @GetMapping("/is-crossed")
    public ResponseEntity<Boolean> isCrossed() {
        return ResponseEntity.ok(orderBook.isCrossed());
    }

    @GetMapping("/check-invariants")
    public ResponseEntity<Boolean> checkInvariants() {
        return ResponseEntity.ok(orderBook.checkInvariants());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuantity", orderBook.getTotalQuantity());
        stats.put("isCrossed", orderBook.isCrossed());
        stats.put("invariantsValid", orderBook.checkInvariants());

        long[] bid = orderBook.getBestBid();
        long[] ask = orderBook.getBestAsk();
        stats.put("bestBid", bid != null ? bid[0] : null);
        stats.put("bestAsk", ask != null ? ask[0] : null);
        stats.put("spread", (bid != null && ask != null) ? ask[0] - bid[0] : null);
        return ResponseEntity.ok(stats);
    }

    /**
     * Returns full depth snapshot (up to 20 levels each side) as JSON.
     * The frontend connects to this via REST polling and WebSocket for live updates.
     */
    @GetMapping("/depth")
    public ResponseEntity<Map<String, Object>> getDepth(
            @RequestParam(name = "levels", defaultValue = "20") int levels) {
        long[][] bids = orderBook.getBidLevels(levels);
        long[][] asks = orderBook.getAskLevels(levels);

        List<Map<String, Object>> bidList = new ArrayList<>();
        for (long[] level : bids) {
            Map<String, Object> m = new HashMap<>();
            m.put("price", level[0]);
            m.put("quantity", level[1]);
            bidList.add(m);
        }

        List<Map<String, Object>> askList = new ArrayList<>();
        for (long[] level : asks) {
            Map<String, Object> m = new HashMap<>();
            m.put("price", level[0]);
            m.put("quantity", level[1]);
            askList.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bids", bidList);
        result.put("asks", askList);

        long[] bestBid = orderBook.getBestBid();
        long[] bestAsk = orderBook.getBestAsk();
        result.put("bestBid", bestBid != null ? bestBid[0] : null);
        result.put("bestAsk", bestAsk != null ? bestAsk[0] : null);
        result.put("spread", (bestBid != null && bestAsk != null) ? bestAsk[0] - bestBid[0] : null);

        return ResponseEntity.ok(result);
    }
}
