package com.lobsimulator.engine;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A simple object pool for TradeEvents to reduce GC pressure during high-throughput matching.
 */
public class TradeEventPool {
    private final BlockingQueue<TradeEvent> pool;
    private final int maxPoolSize;

    public TradeEventPool(int initialSize, int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        this.pool = new ArrayBlockingQueue<>(maxPoolSize);
        for (int i = 0; i < initialSize; i++) {
            pool.offer(new TradeEvent(0, 0, true, "", "", 0));
        }
    }

    /**
     * Borrows a TradeEvent from the pool or creates a new one if the pool is empty.
     */
    public TradeEvent borrow(long price, int qty, boolean buySide, String makerId, String takerId, long timestamp) {
        TradeEvent event = pool.poll();
        if (event == null) {
            return new TradeEvent(price, qty, buySide, makerId, takerId, timestamp);
        }
        event.reset(price, qty, buySide, makerId, takerId, timestamp);
        return event;
    }

    /**
     * Returns a TradeEvent to the pool for reuse.
     */
    public void release(TradeEvent event) {
        if (event != null && pool.size() < maxPoolSize) {
            pool.offer(event);
        }
    }

    public int getAvailable() {
        return pool.size();
    }
}
