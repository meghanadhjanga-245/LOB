package com.lobsimulator.engine;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Object pool for {@link Order} instances to reduce GC pressure during high-throughput matching.
 * <p>
 * Because {@code buySide} is a {@code final} field on {@link Order}, the pool maintains
 * separate queues for buy-side and sell-side orders so that pooled objects are always
 * reused with the correct, immutable side flag.
 * </p>
 */
public class OrderPool {

    private final BlockingQueue<Order> buyPool;
    private final BlockingQueue<Order> sellPool;
    private final int maxPoolSize;

    /**
     * Creates a new pool pre-populated with the given initial size.
     *
     * @param initialSize  number of objects to pre-allocate (split equally between sides)
     * @param maxPoolSize  maximum total objects held in the pool at any time
     */
    public OrderPool(int initialSize, int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        this.buyPool  = new ArrayBlockingQueue<>(maxPoolSize);
        this.sellPool = new ArrayBlockingQueue<>(maxPoolSize);
        int half = initialSize / 2;
        for (int i = 0; i < half; i++) {
            buyPool.offer(new Order("", true,  0L, 1, 0L, OrderType.LIMIT));
            sellPool.offer(new Order("", false, 0L, 1, 0L, OrderType.LIMIT));
        }
    }

    /**
     * Borrows an Order from the pool, or allocates a new one if the pool is empty.
     * The returned order is fully reset to the supplied parameters.
     *
     * @param orderId   unique order identifier
     * @param buySide   true for buy, false for sell
     * @param price     limit price in ticks
     * @param quantity  order quantity
     * @param timestamp nanosecond timestamp of order creation
     * @param type      order type (LIMIT, MARKET, IOC, FOK)
     * @return a ready-to-use Order instance
     */
    public Order borrow(String orderId, boolean buySide, long price, int quantity, long timestamp, OrderType type) {
        BlockingQueue<Order> pool = buySide ? buyPool : sellPool;
        Order order = pool.poll();
        if (order == null) {
            return new Order(orderId, buySide, price, quantity, timestamp, type);
        }
        order.reset(orderId, price, quantity, timestamp, type);
        return order;
    }

    /**
     * Returns an Order to the pool for future reuse.
     * The order must NOT be used after it has been released.
     *
     * @param order the order to return; null is silently ignored
     */
    public void release(Order order) {
        if (order == null) return;
        BlockingQueue<Order> pool = order.isBuySide() ? buyPool : sellPool;
        if (pool.size() < maxPoolSize) {
            pool.offer(order);
        }
    }

    /**
     * Returns the total number of orders currently available across both pools.
     */
    public int getAvailable() {
        return buyPool.size() + sellPool.size();
    }
}
