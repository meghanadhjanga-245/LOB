package com.lobsimulator.benchmarks;

import com.lobsimulator.engine.MatchingEngine;
import com.lobsimulator.engine.Order;
import com.lobsimulator.flow.OrderFlowGenerator;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * End-to-end benchmark measuring order flow generation and processing.
 * <p>
 * Measures the complete pipeline: generate orders -> process through matching engine.
 * </p>
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class EndToEndBenchmark {

    private MatchingEngine matchingEngine;
    private OrderFlowGenerator flowGenerator;
    private long orderIdCounter;

    @Setup(Level.Trial)
    public void setUp() {
        matchingEngine = new MatchingEngine();
        // Create a flow generator with moderate parameters
        flowGenerator = new OrderFlowGenerator(
                12345L, // seed
                50.0,   // lambda: 50 orders per second
                95,     // minPrice
                105,    // maxPrice
                1,      // minQuantity
                100,    // maxQuantity
                0.5,    // buyRatio
                0.1     // marketOrderRatio
        );
        orderIdCounter = 0;
    }

    @Benchmark
    public void generateAndProcessOrder() {
        // Generate a synthetic order
        Order order = flowGenerator.generateOrder();

        // Process it through the matching engine
        matchingEngine.processOrder(order);

        orderIdCounter++;
    }
}