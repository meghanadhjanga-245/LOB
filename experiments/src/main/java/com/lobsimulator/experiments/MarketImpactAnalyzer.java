package com.lobsimulator.experiments;

import com.lobsimulator.engine.Order;
import com.lobsimulator.engine.OrderBook;
import com.lobsimulator.engine.OrderType;
import com.lobsimulator.engine.TradeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes market impact by executing market orders of different sizes
 * and measuring the execution cost (slippage).
 * <p>
 * The market impact curve shows how execution cost grows with order size.
 * </p>
 */
public class MarketImpactAnalyzer {

    /**
     * Result of a single market impact test.
     */
    public static class ImpactResult {
        public final int orderSize;           // size of market order executed
        public final double averagePrice;     // volume-weighted average price
        public final double slippage;         // execution cost vs mid-price
        public final double slippageBps;      // slippage in basis points
        public final List<TradeEvent> trades; // individual trades that occurred

        public ImpactResult(int orderSize, double averagePrice, double slippage,
                            double slippageBps, List<TradeEvent> trades) {
            this.orderSize = orderSize;
            this.averagePrice = averagePrice;
            this.slippage = slippage;
            this.slippageBps = slippageBps;
            this.trades = trades;
        }
    }

    /**
     * Analyzes market impact by executing market orders of increasing sizes
     * against the given order book and measuring execution costs.
     *
     * @param orderBook   the order book to test against (will be modified)
     * @param isBuy       true for buy market orders, false for sell
     * @param maxSize     maximum order size to test
     * @param stepSize    increment between tested sizes
     * @return list of impact results for each tested order size
     */
    public List<ImpactResult> analyzeMarketImpact(OrderBook orderBook, boolean isBuy,
                                                  int maxSize, int stepSize) {
        List<ImpactResult> results = new ArrayList<>();

        // Test order sizes from stepSize to maxSize in increments of stepSize
        for (int size = stepSize; size <= maxSize; size += stepSize) {
            // Create a copy of the order book for this test to avoid modifying the original
            OrderBook testBook = copyOrderBook(orderBook);

            // Execute market order of given size
            List<TradeEvent> trades = executeMarketOrder(testBook, isBuy, size);

            // Calculate average execution price
            double avgPrice = calculateAveragePrice(trades);

            // Get mid-price before execution for slippage calculation
            double midPrice = calculateMidPrice(orderBook);

            // Calculate slippage
            double slippage = 0.0;
            if (midPrice > 0 && !trades.isEmpty()) {
                if (isBuy) {
                    // For buys: slippage = (avg price - mid price) / mid price
                    slippage = (avgPrice - midPrice) / midPrice;
                } else {
                    // For sells: slippage = (mid price - avg price) / mid price
                    slippage = (midPrice - avgPrice) / midPrice;
                }
            }

            double slippageBps = slippage * 10000; // convert to basis points

            results.add(new ImpactResult(size, avgPrice, slippage, slippageBps, trades));
        }

        return results;
    }

    /**
     * Creates a deep copy of an order book.
     * <p>
     * Note: This is a simplified copy that recreates the order book state.
     * For a production system, a more sophisticated copying mechanism might be needed.
     * </p>
     *
     * @param original the order book to copy
     * @return a copy of the order book
     */
    private OrderBook copyOrderBook(OrderBook original) {
        return original.snapshot();
    }

    /**
     * Executes a market order against the order book and returns the trades.
     *
     * @param orderBook the order book to execute against
     * @param isBuy     true for buy order, false for sell order
     * @param size      the size of the market order to execute
     * @return list of trades generated from executing the market order
     */
    private List<TradeEvent> executeMarketOrder(OrderBook orderBook, boolean isBuy, int size) {
        // Create a market order
        Order marketOrder = new Order(
                "IMPACT_" + System.nanoTime(), // order ID
                isBuy,                         // side
                0,                             // price (irrelevant for market order)
                size,                          // quantity
                System.nanoTime(),             // timestamp
                OrderType.MARKET               // isMarket
        );

        // Process the order through the matching engine
        // We need access to matching engine - let's create one
        // In a full system, we'd inject this dependency
        // For now, we'll create a simple matching engine locally

        // Since we can't easily access the matching engine's processOrder method
        // without modifying the architecture, let's implement the core logic here
        // or use reflection. Given the constraints, let's create a matching engine.

        // Actually, let's check if we can create a matching engine and use it
        // The matching engine needs an order book - we can pass our order book
        // But the matching engine has its own order book field...

        // Let's use the approach: we'll directly use the matching engine logic
        // by creating a matching engine and replacing its order book
        // This is not ideal but works for our purposes

        // Better approach: since OrderBook and MatchingEngine are in the same package,
        // and we're in a different package, we can access public methods.
        // Let's create a matching engine and set its order book to our copy
        // via reflection or by modifying the OrderBook to allow setting.

        // Given time constraints, let's implement a simplified version
        // that uses the OrderBook methods directly to simulate market order execution

        return executeMarketOrderSimplified(orderBook, isBuy, size);
    }

    /**
     * Simplified market order execution using OrderBook methods directly.
     * This duplicates some logic from MatchingEngine but works within constraints.
     *
     * @param orderBook the order book to execute against
     * @param isBuy     true for buy order, false for sell order
     * @param size      the size of the market order to execute
     * @return list of trades generated
     */
    List<TradeEvent> executeMarketOrderSimplified(OrderBook orderBook, boolean isBuy, int size) {
        List<TradeEvent> trades = new ArrayList<>();
        int remainingQty = size;
        long timestamp = System.nanoTime();
        String takerOrderId = "IMPACT_" + timestamp;

        while (remainingQty > 0) {
            if (isBuy) {
                // Buy market order: lift asks (execute against ask side)
                long[] bestAsk = orderBook.getBestAsk();
                if (bestAsk == null) {
                    // No asks left to execute against
                    break;
                }
                long askPrice = bestAsk[0];
                int askQty = (int) bestAsk[1];

                // We can execute at this price level
                int qtyToExecute = Math.min(remainingQty, askQty);

                // Find orders at this price level and execute them in FIFO order
                int executedAtLevel = 0;
                while (executedAtLevel < qtyToExecute) {
                    Order restingOrder = orderBook.getFirstOrder(false, askPrice); // SELL orders
                    if (restingOrder == null) {
                        break; // Should not happen if askQty > 0
                    }

                    int restingQty = restingOrder.getQuantity();
                    int tradeQty = Math.min(restingQty, qtyToExecute - executedAtLevel);

                    // Create trade
                    TradeEvent trade = new TradeEvent(
                            askPrice,           // trade price (ask price)
                            tradeQty,           // quantity
                            true,               // aggressor is BUY
                            restingOrder.getOrderId(), // maker order (resting)
                            takerOrderId,       // taker order (our market order)
                            timestamp           // trade timestamp
                    );
                    trades.add(trade);

                    // Reduce the resting order
                    orderBook.reduceOrder(restingOrder.getOrderId(), tradeQty);

                    executedAtLevel += tradeQty;
                    remainingQty -= tradeQty;
                }
            } else {
                // Sell market order: hit bids (execute against bid side)
                long[] bestBid = orderBook.getBestBid();
                if (bestBid == null) {
                    // No bids left to execute against
                    break;
                }
                long bidPrice = bestBid[0];
                int bidQty = (int) bestBid[1];

                // We can execute at this price level
                int qtyToExecute = Math.min(remainingQty, bidQty);

                // Find orders at this price level and execute them in FIFO order
                int executedAtLevel = 0;
                while (executedAtLevel < qtyToExecute) {
                    Order restingOrder = orderBook.getFirstOrder(true, bidPrice); // BUY orders
                    if (restingOrder == null) {
                        break; // Should not happen if bidQty > 0
                    }

                    int restingQty = restingOrder.getQuantity();
                    int tradeQty = Math.min(restingQty, qtyToExecute - executedAtLevel);

                    // Create trade
                    TradeEvent trade = new TradeEvent(
                            bidPrice,           // trade price (bid price)
                            tradeQty,           // quantity
                            false,              // aggressor is SELL
                            restingOrder.getOrderId(), // maker order (resting)
                            takerOrderId,       // taker order (our market order)
                            timestamp           // trade timestamp
                    );
                    trades.add(trade);

                    // Reduce the resting order
                    orderBook.reduceOrder(restingOrder.getOrderId(), tradeQty);

                    executedAtLevel += tradeQty;
                    remainingQty -= tradeQty;
                }
            }
        }

        return trades;
    }

    /**
     * Calculates the volume-weighted average price of a list of trades.
     *
     * @param trades list of trades
     * @return volume-weighted average price, or 0.0 if no trades
     */
    private double calculateAveragePrice(List<TradeEvent> trades) {
        if (trades.isEmpty()) {
            return 0.0;
        }

        long totalQuantity = 0;
        double totalValue = 0.0;

        for (TradeEvent trade : trades) {
            int quantity = trade.getQuantity();
            long price = trade.getPrice();
            totalQuantity += quantity;
            totalValue += price * quantity;
        }

        return totalValue / totalQuantity;
    }

    /**
     * Calculates the mid-price from the best bid and best ask.
     *
     * @param orderBook the order book to query
     * @return mid-price, or 0.0 if one side is empty
     */
    double calculateMidPrice(OrderBook orderBook) {
        long[] bestBid = orderBook.getBestBid();
        long[] bestAsk = orderBook.getBestAsk();

        if (bestBid == null || bestAsk == null) {
            return 0.0; // Cannot calculate mid-price if one side is empty
        }

        return (bestBid[0] + bestAsk[0]) / 2.0;
    }
}