package com.lobsimulator.flow.marketdata;

import com.lobsimulator.engine.Order;
import com.lobsimulator.engine.OrderType;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Imports trade data (time and sales) from CSV files and converts trades to market orders.
 * <p>
 * This allows replaying real market transactions as market orders in the simulator.
 * </p>
 */
public class TradeCSVImporter implements MarketDataImporter {
    private final String csvFilePath;
    private final long seed;
    private final double volumeScale; // scale factor for order quantities
    private final double delayFactor; // factor to control timing (0.0 = real-time, >0.0 = delayed)
    private final int minQuantity; // minimum order quantity
    private final int maxQuantity; // maximum order quantity

    /**
     * Creates a trade CSV importer.
     *
     * @param csvFilePath    path to the CSV file with trade data
     * @param seed           random seed for reproducible processing
     * @param volumeScale    scale factor to apply to trade quantities (1.0 = actual size)
     * @param delayFactor    timing factor (0.0 = no delay, higher = more delay between orders)
     * @param minQuantity    minimum order quantity
     * @param maxQuantity    maximum order quantity
     */
    public TradeCSVImporter(String csvFilePath, long seed,
                            double volumeScale, double delayFactor,
                            int minQuantity, int maxQuantity) {
        this.csvFilePath = csvFilePath;
        this.seed = seed;
        this.volumeScale = volumeScale;
        this.delayFactor = delayFactor;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
    }

    @Override
    public List<Order> importOrders() throws Exception {
        List<Order> orders = new ArrayList<>();
        long orderCounter = 0;
        Random random = new Random(seed);
        long timestampNs = System.nanoTime(); // starting timestamp

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                // Skip empty lines
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }

                // Handle potential header line
                if (isHeader && (line.toLowerCase().contains("date") ||
                                 line.toLowerCase().contains("time") ||
                                 line.toLowerCase().contains("price") ||
                                 line.toLowerCase().contains("size"))) {
                    isHeader = false;
                    continue;
                }

                // Parse CSV line: expecting format: timestamp,price,size,side
                // or: date,time,price,size,side
                String[] values = line.split(",");
                if (values.length < 4) {
                    continue; // Skip malformed lines
                }

                try {
                    // Parse trade data (adjust parsing based on actual format)
                    String priceStr = values[values.length - 3];
                    String sizeStr = values[values.length - 2];
                    String sideStr = values[values.length - 1].toUpperCase().trim();

                    double price = Double.parseDouble(priceStr);
                    double size = Double.parseDouble(sizeStr);
                    boolean buySide = sideStr.equals("B") || sideStr.equals("BUY") ||
                                     sideStr.equals("1") || sideStr.equals("+");

                    // Apply volume scaling
                    int quantity = Math.max(minQuantity,
                            Math.min(maxQuantity,
                                    (int) Math.round(size * volumeScale)));

                    // Ensure quantity is within bounds
                    quantity = Math.max(minQuantity, Math.min(maxQuantity, quantity));

                    // Calculate timestamp with optional delay
                    long delayNanos = (long) (delayFactor * 1_000_000_000 * random.nextDouble());
                    long timestamp = timestampNs + delayNanos;

                    Order order = new Order(
                            "TRADE_" + orderCounter++,
                            buySide,
                            (long) Math.round(price),
                            quantity,
                            timestamp,
                            OrderType.MARKET // market order (replaying real trades as market orders)
                    );

                    orders.add(order);

                    // Advance base timestamp
                    timestampNs += 100000000; // 100ms between trades (adjust as needed)

                } catch (NumberFormatException e) {
                    // Skip lines that can't be parsed
                    continue;
                }
            }
        }

        return orders;
    }

    @Override
    public String getSourceDescription() {
        return "Trade CSV file: " + csvFilePath;
    }
}