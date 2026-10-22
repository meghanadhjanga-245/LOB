package com.lobsimulator.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import java.util.Random;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for OrderBook.
 * Includes unit tests and property-based tests.
 */
class OrderBookTest {

    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook();
    }

    @Test
    void testAddAndCancelOrder() {
        String id = orderBook.addOrder(true, 100, 10, false); // BUY at 100
        assertNotNull(id);
        assertTrue(orderBook.checkInvariants());

        long[] bestBid = orderBook.getBestBid();
        assertNotNull(bestBid);
        assertEquals(100, bestBid[0]);
        assertEquals(10, bestBid[1]);

        boolean cancelled = orderBook.cancelOrder(id);
        assertTrue(cancelled);
        assertTrue(orderBook.checkInvariants());

        assertNull(orderBook.getBestBid());
        assertNull(orderBook.getBestAsk());
    }

    @Test
    void testReduceOrder() {
        String id = orderBook.addOrder(false, 101, 20, false); // SELL at 101
        assertTrue(orderBook.checkInvariants());

        long[] bestAsk = orderBook.getBestAsk();
        assertNotNull(bestAsk);
        assertEquals(101, bestAsk[0]);
        assertEquals(20, bestAsk[1]);

        // Reduce by 5
        boolean reduced = orderBook.reduceOrder(id, 5);
        assertTrue(reduced);
        assertTrue(orderBook.checkInvariants());

        bestAsk = orderBook.getBestAsk();
        assertNotNull(bestAsk);
        assertEquals(101, bestAsk[0]);
        assertEquals(15, bestAsk[1]);

        // Reduce remaining 15 -> should remove order
        reduced = orderBook.reduceOrder(id, 15);
        assertTrue(reduced);
        assertTrue(orderBook.checkInvariants());

        assertNull(orderBook.getBestAsk());
    }

    @Test
    void testChangeOrderPrice() {
        String id = orderBook.addOrder(true, 100, 10, false); // BUY at 100
        assertTrue(orderBook.checkInvariants());

        String newId = orderBook.changeOrderPrice(id, 105);
        assertNotNull(newId);
        assertNotEquals(id, newId);
        assertTrue(orderBook.checkInvariants());

        // Original order should be gone; best bid should reflect the new order
        long[] bestBid = orderBook.getBestBid();
        assertNotNull(bestBid);
        assertEquals(105, bestBid[0]);
        assertEquals(10, bestBid[1]);

        // Ensure the old orderId is not in the map
        // (We don't expose a method to check orderMap directly, but we can infer from best bid)
    }

    @Test
    void testMultipleOrdersSamePrice() {
        String id1 = orderBook.addOrder(true, 100, 5, false);
        String id2 = orderBook.addOrder(true, 100, 10, false);
        assertTrue(orderBook.checkInvariants());

        long[] bestBid = orderBook.getBestBid();
        assertNotNull(bestBid);
        assertEquals(100, bestBid[0]);
        assertEquals(15, bestBid[1]); // 5 + 10

        // Cancel first order
        orderBook.cancelOrder(id1);
        assertTrue(orderBook.checkInvariants());

        bestBid = orderBook.getBestBid();
        assertNotNull(bestBid);
        assertEquals(100, bestBid[0]);
        assertEquals(10, bestBid[1]); // only second order remains

        // Cancel second order
        orderBook.cancelOrder(id2);
        assertTrue(orderBook.checkInvariants());
        assertNull(orderBook.getBestBid());
    }

    @Test
    void testBidAskSpread() {
        String bidId = orderBook.addOrder(true, 100, 5, false);
        String askId = orderBook.addOrder(false, 101, 5, false);
        assertTrue(orderBook.checkInvariants());
        assertFalse(orderBook.isCrossed());

        long[] bestBid = orderBook.getBestBid();
        long[] bestAsk = orderBook.getBestAsk();
        assertNotNull(bestBid);
        assertNotNull(bestAsk);
        assertEquals(100, bestBid[0]);
        assertEquals(101, bestAsk[0]);

        // Attempt to cross the book by changing bid price to 102 (should be prevented)
        String newBidId = orderBook.changeOrderPrice(bidId, 102);
        assertNull(newBidId); // change prevented
        assertTrue(orderBook.checkInvariants()); // book should still be valid
        assertFalse(orderBook.isCrossed()); // book should not be crossed

        // Verify the original bid order still exists at price 100
        long[] bestBidAfter = orderBook.getBestBid();
        long[] bestAskAfter = orderBook.getBestAsk();
        assertNotNull(bestBidAfter);
        assertNotNull(bestAskAfter);
        assertEquals(100, bestBidAfter[0]);
        assertEquals(5, bestBidAfter[1]);
        assertEquals(101, bestAskAfter[0]);
        assertEquals(5, bestAskAfter[1]);
    }

    /**
     * Property-based test: random sequences of add, cancel, reduce, change-price should maintain invariants.
     */
    @Property
    void propertyRandomOperationsMaintainInvariants(
            @ForAll int seed,
            @ForAll int operationCount) {
        int maxOps = 1000;
        int ops = operationCount > maxOps ? maxOps : operationCount;
        if (ops <= 0) {
            return;
        }
        java.util.Random random = new java.util.Random(seed);

        OrderBook book = new OrderBook();
        Set<String> liveOrderIds = new HashSet<>();

        for (int i = 0; i < ops; i++) {
            int op = random.nextInt(4); // 0: add, 1: cancel, 2: reduce, 3: change-price
            switch (op) {
                case 0 -> {
                    // Add order
                    boolean buySide = random.nextBoolean();
                    long price = 90 + random.nextInt(20); // prices between 90 and 110
                    int quantity = 1 + random.nextInt(100); // quantity between 1 and 100
                    try {
                        String id = book.addOrder(buySide, price, quantity, false);
                        liveOrderIds.add(id);
                    } catch (IllegalArgumentException e) {
                        // Order would cross the book or invalid quantity; ignore this operation
                        // (but note: we already checked quantity > 0, so it must be crossing)
                    }
                }
                case 1 -> {
                    // Cancel order
                    if (!liveOrderIds.isEmpty()) {
                        Object[] ids = liveOrderIds.toArray();
                        String id = (String) ids[random.nextInt(ids.length)];
                        boolean cancelled = book.cancelOrder(id);
                        if (cancelled) {
                            liveOrderIds.remove(id);
                        }
                    }
                }
                case 2 -> {
                    // Reduce order
                    if (!liveOrderIds.isEmpty()) {
                        Object[] ids = liveOrderIds.toArray();
                        String id = (String) ids[random.nextInt(ids.length)];
                        // Get current quantity? We don't have a method to get order quantity.
                        // For simplicity, we reduce by a fixed small amount, but ensure we don't go negative.
                        // We'll reduce by 1, and if the order quantity becomes zero, it will be removed.
                        // However, we don't know the quantity. We'll assume quantity is at least 1.
                        boolean reduced = book.reduceOrder(id, 1);
                        if (!reduced) {
                            // If reduce failed (maybe order not found?), remove from live set just in case
                            liveOrderIds.remove(id);
                        }
                        // If the order is still live, we keep it; if removed, the reduceOrder would have cancelled it.
                        // We don't have a way to know if the order was removed, so we'll rely on the fact that
                        // reduceOrder returns true if the order existed and reduction succeeded.
                        // If it returns false, we treat it as if the order was not found and remove from live set.
                        // This is a simplification.
                    }
                }
                case 3 -> {
                    // Change price
                    if (!liveOrderIds.isEmpty()) {
                        Object[] ids = liveOrderIds.toArray();
                        String id = (String) ids[random.nextInt(ids.length)];
                        long newPrice = 90 + random.nextInt(20);
                        String newId = book.changeOrderPrice(id, newPrice);
                        if (newId != null) {
                            liveOrderIds.remove(id);
                            liveOrderIds.add(newId);
                        }
                    }
                }
            }
            // Check invariants after each operation
            assertTrue(book.checkInvariants(), "Invariant violated after operation " + i);
        }
    }
}