package com.lobsimulator.flow;

import com.lobsimulator.engine.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for OrderFlowGenerator.
 */
class OrderFlowGeneratorTest {

    private OrderFlowGenerator generator;

    @BeforeEach
    void setUp() {
        // Create a generator with fixed seed for reproducible tests
        generator = new OrderFlowGenerator(
                12345L, // seed
                10.0,   // lambda: 10 orders per second
                90,     // minPrice
                110,    // maxPrice
                1,      // minQuantity
                100,    // maxQuantity
                0.5,    // buyRatio: 50% buy, 50% sell
                0.1     // marketOrderRatio: 10% market, 90% limit
        );
    }

    @Test
    void testGenerateOrderHasCorrectProperties() {
        Order order = generator.generateOrder();

        // Order should not be null
        assertNotNull(order);

        // Order ID should start with FLOW_
        assertTrue(order.getOrderId().startsWith("FLOW_"));

        // Price should be within bounds
        long price = order.getPrice();
        assertTrue(price >= 90 && price <= 110);

        // Quantity should be within bounds
        int quantity = order.getQuantity();
        assertTrue(quantity >= 1 && quantity <= 100);

        // Side should be either BUY or SELL (always true for boolean)
        // Market flag should be either true or false (always true for boolean)
    }

    @Test
    void testGenerateOrderWithSeedIsReproducible() {
        // Create two generators with same seed
        OrderFlowGenerator gen1 = new OrderFlowGenerator(999L, 5.0, 90, 110, 1, 100, 0.5, 0.1);
        OrderFlowGenerator gen2 = new OrderFlowGenerator(999L, 5.0, 90, 110, 1, 100, 0.5, 0.1);

        // Generate sequences of orders
        Order order1a = gen1.generateOrder();
        Order order1b = gen1.generateOrder();
        Order order2a = gen2.generateOrder();
        Order order2b = gen2.generateOrder();

        // Orders should be identical
        assertEquals(order1a.getOrderId(), order2a.getOrderId());
        assertEquals(order1a.isBuySide(), order2a.isBuySide());
        assertEquals(order1a.getPrice(), order2a.getPrice());
        assertEquals(order1a.getQuantity(), order2a.getQuantity());
        assertEquals(order1a.isMarket(), order2a.isMarket());
        assertEquals(order1a.getTimestamp(), order2a.getTimestamp());

        assertEquals(order1b.getOrderId(), order2b.getOrderId());
        assertEquals(order1b.isBuySide(), order2b.isBuySide());
        assertEquals(order1b.getPrice(), order2b.getPrice());
        assertEquals(order1b.getQuantity(), order2b.getQuantity());
        assertEquals(order1b.isMarket(), order2b.isMarket());
        assertEquals(order1b.getTimestamp(), order2b.getTimestamp());
    }

    @Test
    void testResetGenerator() {
        // Generate an order
        Order orderBeforeReset = generator.generateOrder();
        long timeBeforeReset = generator.getNextOrderTimeNs();

        // Reset with same seed
        generator.reset(12345L);

        // Generate order after reset
        Order orderAfterReset = generator.generateOrder();
        long timeAfterReset = generator.getNextOrderTimeNs();

        // Should be identical to first order
        assertEquals(orderBeforeReset.getOrderId(), orderAfterReset.getOrderId());
        assertEquals(orderBeforeReset.isBuySide(), orderAfterReset.isBuySide());
        assertEquals(orderBeforeReset.getPrice(), orderAfterReset.getPrice());
        assertEquals(orderBeforeReset.getQuantity(), orderAfterReset.getQuantity());
        assertEquals(orderBeforeReset.isMarket(), orderAfterReset.isMarket());
        assertEquals(orderBeforeReset.getTimestamp(), orderAfterReset.getTimestamp());

        // Next order time should also be reset
        assertEquals(timeBeforeReset, timeAfterReset);
    }

    @Test
    void testMarketOrderRatio() {
        // Create generator with 100% market orders
        OrderFlowGenerator marketGen = new OrderFlowGenerator(111L, 1.0, 90, 110, 1, 100, 0.5, 1.0);

        // All orders should be market orders
        for (int i = 0; i < 100; i++) {
            Order order = marketGen.generateOrder();
            assertTrue(order.isMarket(), "Order " + i + " should be market order");
        }

        // Create generator with 0% market orders (all limit)
        OrderFlowGenerator limitGen = new OrderFlowGenerator(222L, 1.0, 90, 110, 1, 100, 0.5, 0.0);

        // All orders should be limit orders
        for (int i = 0; i < 100; i++) {
            Order order = limitGen.generateOrder();
            assertFalse(order.isMarket(), "Order " + i + " should be limit order");
        }
    }

    @Test
    void testBuyRatio() {
        // Create generator with 100% buy orders
        OrderFlowGenerator buyGen = new OrderFlowGenerator(333L, 1.0, 90, 110, 1, 100, 1.0, 0.1);

        // All orders should be buy orders
        for (int i = 0; i < 100; i++) {
            Order order = buyGen.generateOrder();
            assertTrue(order.isBuySide(), "Order " + i + " should be buy order");
        }

        // Create generator with 0% buy orders (all sell)
        OrderFlowGenerator sellGen = new OrderFlowGenerator(444L, 1.0, 90, 110, 1, 100, 0.0, 0.1);

        // All orders should be sell orders
        for (int i = 0; i < 100; i++) {
            Order order = sellGen.generateOrder();
            assertFalse(order.isBuySide(), "Order " + i + " should be sell order");
        }
    }

    @Test
    void testPriceBounds() {
        // Create generator with narrow price range
        OrderFlowGenerator gen = new OrderFlowGenerator(555L, 1.0, 100, 100, 1, 100, 0.5, 0.1);

        // All orders should have price exactly 100
        for (int i = 0; i < 50; i++) {
            Order order = gen.generateOrder();
            assertEquals(100, order.getPrice(), "Order " + i + " should have price 100");
        }
    }

    @Test
    void testQuantityBounds() {
        // Create generator with fixed quantity
        OrderFlowGenerator gen = new OrderFlowGenerator(666L, 1.0, 90, 110, 50, 50, 0.5, 0.1);

        // All orders should have quantity exactly 50
        for (int i = 0; i < 50; i++) {
            Order order = gen.generateOrder();
            assertEquals(50, order.getQuantity(), "Order " + i + " should have quantity 50");
        }
    }

    @Test
    void testInterArrivalTimesArePositive() {
        // Just verify that we can generate multiple orders without issues
        long lastTime = 0;
        for (int i = 0; i < 1000; i++) {
            Order order = generator.generateOrder();
            long time = order.getTimestamp();

            // Time should be positive and increasing (Poisson process)
            assertTrue(time > 0, "Timestamp should be positive");
            assertTrue(time >= lastTime, "Timestamp should be non-decreasing");
            lastTime = time;
        }
    }
}