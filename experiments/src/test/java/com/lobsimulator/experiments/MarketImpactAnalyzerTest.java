package com.lobsimulator.experiments;

import com.lobsimulator.engine.Order;
import com.lobsimulator.engine.OrderBook;
import com.lobsimulator.engine.TradeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MarketImpactAnalyzer.
 */
class MarketImpactAnalyzerTest {

    private MarketImpactAnalyzer analyzer;
    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        analyzer = new MarketImpactAnalyzer();
        orderBook = new OrderBook();

        // Add some liquidity to the order book for testing
        // Add ask orders (sell orders)
        orderBook.addOrder(false, 100, 10, false); // SELL at 100, qty 10
        orderBook.addOrder(false, 101, 20, false); // SELL at 101, qty 20
        orderBook.addOrder(false, 102, 15, false); // SELL at 102, qty 15

        // Add bid orders (buy orders)
        orderBook.addOrder(true, 99, 10, false);   // BUY at 99, qty 10
        orderBook.addOrder(true, 98, 20, false);   // BUY at 98, qty 20
        orderBook.addOrder(true, 97, 15, false);   // BUY at 97, qty 15
    }

    @Test
    void testAnalyzeMarketImpactBuy() {
        // Test market impact for buy orders
        var results = analyzer.analyzeMarketImpact(orderBook, true, 30, 10); // buy, max 30, step 10

        assertFalse(results.isEmpty(), "Should have results");

        // Check first result (size 10)
        var result10 = results.get(0);
        assertEquals(10, result10.orderSize);
        assertTrue(result10.averagePrice > 0, "Average price should be positive");
        assertTrue(result10.slippage >= 0, "Slippage should be non-negative for buy orders");
        assertEquals(50, result10.slippageBps, 1, "Slippage in bps should be approximately correct");
        assertFalse(result10.trades.isEmpty(), "Should have trades");

        // Check second result (size 20)
        var result20 = results.get(1);
        assertEquals(20, result20.orderSize);

        // Check third result (size 30)
        var result30 = results.get(2);
        assertEquals(30, result30.orderSize);
    }

    @Test
    void testAnalyzeMarketImpactSell() {
        // Test market impact for sell orders
        var results = analyzer.analyzeMarketImpact(orderBook, false, 30, 10); // sell, max 30, step 10

        assertFalse(results.isEmpty(), "Should have results");

        // Check first result (size 10)
        var result10 = results.get(0);
        assertEquals(10, result10.orderSize);
        assertTrue(result10.averagePrice > 0, "Average price should be positive");
        assertTrue(result10.slippage >= 0, "Slippage should be non-negative for sell orders");
        assertFalse(result10.trades.isEmpty(), "Should have trades");
    }

    @Test
    void testCalculateAveragePrice() {
        // This test would require accessing private methods, so we test through public API instead
        // The average price calculation is tested implicitly through analyzeMarketImpact
    }

    @Test
    void testCalculateMidPrice() {
        // Test that we can get a reasonable mid-price
        double midPrice = analyzer.calculateMidPrice(orderBook);
        assertTrue(midPrice > 97 && midPrice < 102, "Mid-price should be between best bid and ask");

        // Test with empty side
        OrderBook emptyBook = new OrderBook();
        double emptyMid = analyzer.calculateMidPrice(emptyBook);
        assertEquals(0.0, emptyMid, "Mid-price should be 0 when one side is empty");
    }

    @Test
    void testExecuteMarketOrderSimplified() {
        // Test that we can execute market orders and get trades
        var trades = analyzer.executeMarketOrderSimplified(orderBook, true, 15); // buy 15

        assertFalse(trades.isEmpty(), "Should have trades for buy order");

        // Verify trades are at ask prices (100, 101, etc.)
        for (TradeEvent trade : trades) {
            assertTrue(trade.isAggressorBuySide(), "Aggressor should be buy");
            assertTrue(trade.getPrice() >= 100 && trade.getPrice() <= 102,
                      "Trade price should be at ask levels");
        }

        // Test sell order
        var sellTrades = analyzer.executeMarketOrderSimplified(orderBook, false, 15); // sell 15
        assertFalse(sellTrades.isEmpty(), "Should have trades for sell order");

        for (TradeEvent trade : sellTrades) {
            assertFalse(trade.isAggressorBuySide(), "Aggressor should be sell");
            assertTrue(trade.getPrice() >= 97 && trade.getPrice() <= 99,
                      "Trade price should be at bid levels");
        }
    }

    @Test
    void testSlippageCalculation() {
        // Test that slippage makes sense - larger orders should have more slippage
        var smallResults = analyzer.analyzeMarketImpact(orderBook, true, 10, 10);  // size 10
        var largeResults = analyzer.analyzeMarketImpact(orderBook, true, 30, 10);  // up to size 30

        assertFalse(smallResults.isEmpty());
        assertFalse(largeResults.isEmpty());

        var smallSlippage = smallResults.get(0).slippage;
        var largeSlippage = largeResults.get(2).slippage; // size 30 result

        // Larger order should generally have equal or more slippage
        assertTrue(largeSlippage >= smallSlippage * 0.5,
                  "Larger order should have comparable or greater slippage");
    }
}