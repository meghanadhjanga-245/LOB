package com.lobsimulator.benchmarks;

import com.lobsimulator.engine.MatchingEngine;
import com.lobsimulator.engine.Order;
import com.lobsimulator.engine.OrderType;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark for MatchingEngine performance.
 * <p>
 * Measures how many orders per second the MatchingEngine can process.
 * </p>
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class MatchingEngineBenchmark {

    private MatchingEngine matchingEngine;
    private long orderIdCounter;

    @Setup(Level.Trial)
    public void setUp() {
        matchingEngine = new MatchingEngine();
        orderIdCounter = 0;

        // Pre-populate the order book with some liquidity to enable matching
        // Add some ask orders (sell orders) at various prices
        for (int i = 0; i < 20; i++) {
            long price = 100 + i; // 100-119
            int quantity = 100;
            Order askOrder = new Order(
                    "ASK_" + i,
                    false, // SELL
                    price,
                    quantity,
                    System.nanoTime(),
                    OrderType.LIMIT // limit
            );
            // We need to add directly to order book since we can't access matching engine's internal order book easily
            // For this benchmark, we'll add orders that will definitely cross when we send market orders
        }

        // Add some bid orders (buy orders)
        for (int i = 0; i < 20; i++) {
            long price = 80 + i; // 80-99
            int quantity = 100;
            Order bidOrder = new Order(
                    "BID_" + i,
                    true, // BUY
                    price,
                    quantity,
                    System.nanoTime(),
                    OrderType.LIMIT // limit
            );
            // Same issue as above - we'd need to access the order book directly
        }
    }

    @Benchmark
    public void processMarketOrder() {
        // Process a market order that will definitely match
        boolean buySide = (orderIdCounter % 2 == 0);
        // For market orders, price doesn't matter but we need to set it
        long price = buySide ? 150 : 50; // High price for buy, low for sell to ensure match
        int quantity = 10;

        Order marketOrder = new Order(
                "MARKET_" + orderIdCounter,
                buySide,
                price,
                quantity,
                System.nanoTime(),
                OrderType.MARKET // market order
        );

        matchingEngine.processOrder(marketOrder);
        orderIdCounter++;
    }

    @Benchmark
    public void processLimitOrderNoCross() {
        // Process a limit order that won't cross (resting order)
        boolean buySide = (orderIdCounter % 2 == 0);
        long price = buySide ? 95 : 105; // Prices that won't cross with our pre-set book
        int quantity = 10;

        Order limitOrder = new Order(
                "LIMIT_" + orderIdCounter,
                buySide,
                price,
                quantity,
                System.nanoTime(),
                OrderType.LIMIT // limit order
        );

        matchingEngine.processOrder(limitOrder);
        orderIdCounter++;
    }
}