package com.lobsimulator.benchmarks;

import com.lobsimulator.engine.OrderBook;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark for OrderBook performance.
 * <p>
 * Measures how many orders per second the OrderBook can handle.
 * </p>
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class OrderBookBenchmark {

    private OrderBook orderBook;
    private long orderIdCounter;

    @Setup(Level.Trial)
    public void setUp() {
        orderBook = new OrderBook();
        orderIdCounter = 0;
    }

    @Benchmark
    public void addLimitOrder() {
        // Add a limit order that won't cross (buy below current ask, sell above current bid)
        // For simplicity, we'll alternate sides and use prices that won't cross initially
        boolean buySide = (orderIdCounter % 2 == 0);
        long price = buySide ? 99 + (orderIdCounter % 10) : 101 + (orderIdCounter % 10); // 99-108 or 101-110
        int quantity = 10 + (int) (orderIdCounter % 20); // 10-29

        orderBook.addOrder(buySide, price, quantity, false);
        orderIdCounter++;
    }

    @Benchmark
    public void addAndCancelOrder() {
        String orderId = "ORDER_" + orderIdCounter;
        boolean buySide = (orderIdCounter % 2 == 0);
        long price = buySide ? 99 + (orderIdCounter % 10) : 101 + (orderIdCounter % 10);
        int quantity = 10 + (int) (orderIdCounter % 20);

        orderBook.addOrder(buySide, price, quantity, false);
        orderBook.cancelOrder(orderId);
        orderIdCounter++;
    }

    @Benchmark
    public void getBestBidAsk() {
        orderBook.getBestBid();
        orderBook.getBestAsk();
    }

    @Benchmark
    public void checkInvariants() {
        orderBook.checkInvariants();
    }
}