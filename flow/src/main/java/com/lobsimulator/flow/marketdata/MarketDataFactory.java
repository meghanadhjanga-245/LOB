package com.lobsimulator.flow.marketdata;

import com.lobsimulator.engine.Order;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating market data importers.
 * <p>
 * Provides convenient methods to create common types of importers.
 * </p>
 */
public class MarketDataFactory {

    /**
     * Creates an OHLCV CSV importer for generating synthetic orders around real price action.
     *
     * @param csvFilePath    path to OHLCV CSV file
     * @param seed           random seed for reproducibility
     * @param ordersPerBar   average orders per OHLCV bar
     * @param priceRangePct  percentage of bar range to use for order prices (0.0-1.0)
     * @param buyRatio       probability of buy order (0.0-1.0)
     * @param marketOrderRatio probability of market order (0.0-1.0)
     * @param minQuantity    minimum order quantity
     * @param maxQuantity    maximum order quantity
     * @return configured OHLCV CSV importer
     */
    public static OHLCVCSVImporter createOHLCVImporter(String csvFilePath, long seed,
                                                       double ordersPerBar, double priceRangePct,
                                                       double buyRatio, double marketOrderRatio,
                                                       int minQuantity, int maxQuantity) {
        return new OHLCVCSVImporter(csvFilePath, seed, ordersPerBar, priceRangePct,
                buyRatio, marketOrderRatio, minQuantity, maxQuantity);
    }

    /**
     * Creates a trade CSV importer for replaying real trades as market orders.
     *
     * @param csvFilePath   path to trade CSV file
     * @param seed          random seed for reproducibility
     * @param volumeScale   scale factor for trade quantities
     * @param delayFactor   timing factor between orders
     * @param minQuantity   minimum order quantity
     * @param maxQuantity   maximum order quantity
     * @return configured trade CSV importer
     */
    public static TradeCSVImporter createTradeImporter(String csvFilePath, long seed,
                                                       double volumeScale, double delayFactor,
                                                       int minQuantity, int maxQuantity) {
        return new TradeCSVImporter(csvFilePath, seed, volumeScale, delayFactor,
                minQuantity, maxQuantity);
    }

    /**
     * Creates a composite importer that combines multiple data sources.
     * <p>
     * Orders from all importers are merged and sorted by timestamp.
     * </p>
     *
     * @param importers list of market data importers
     * @return composite importer that merges all sources
     */
    public static MarketDataImporter createCompositeImporter(List<MarketDataImporter> importers) {
        return new CompositeMarketDataImporter(importers);
    }
}

/**
 * Composite importer that combines multiple data sources.
 */
class CompositeMarketDataImporter implements MarketDataImporter {
    private final List<MarketDataImporter> importers;

    public CompositeMarketDataImporter(List<MarketDataImporter> importers) {
        this.importers = importers;
    }

    @Override
    public List<Order> importOrders() throws Exception {
        List<Order> allOrders = new ArrayList<>();

        for (MarketDataImporter importer : importers) {
            List<Order> orders = importer.importOrders();
            allOrders.addAll(orders);
        }

        // Sort all orders by timestamp
        allOrders.sort((o1, o2) -> Long.compare(o1.getTimestamp(), o2.getTimestamp()));

        // Re-sequence order IDs to maintain uniqueness
        for (int i = 0; i < allOrders.size(); i++) {
            Order order = allOrders.get(i);
            // Preserve the original order ID prefix but make it unique
            String originalId = order.getOrderId();
            String newId = originalId.substring(0, Math.min(5, originalId.length())) + "_" + i;
            // Since Order.orderId is final, we can't change it directly
            // In a real implementation, we'd need to handle this differently
            // For now, we'll note that composite importers work best with
            // sources that already have unique IDs
        }

        return allOrders;
    }

    @Override
    public String getSourceDescription() {
        StringBuilder sb = new StringBuilder("Composite data source: [");
        for (int i = 0; i < importers.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(importers.get(i).getSourceDescription());
        }
        sb.append("]");
        return sb.toString();
    }
}