package com.lobsimulator.engine;

import com.lmax.disruptor.EventHandler;

/**
 * Handles OrderEvents published to the LMAX Disruptor ring buffer.
 * <p>
 * This handler runs on the single consumer thread of the Disruptor,
 * providing a foundation for future fully async order processing.
 * Currently wired but not yet used as the primary processing path
 * (see MatchingEngine.processOrder for direct processing).
 * </p>
 */
public class OrderEventHandler implements EventHandler<OrderEvent> {

    private final OrderBook orderBook;

    public OrderEventHandler(OrderBook orderBook) {
        this.orderBook = orderBook;
    }

    /**
     * Called by the Disruptor consumer thread for each published event.
     *
     * @param event      the OrderEvent from the ring buffer slot
     * @param sequence   the sequence number of this event
     * @param endOfBatch true if this is the last event in the current batch
     */
    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        // TODO (ROADMAP Phase 2): Route event through MatchingEngine here
        // for fully non-blocking, single-threaded pinned matching.
        // Currently processOrder() is called directly; this handler is a stub.
    }
}
