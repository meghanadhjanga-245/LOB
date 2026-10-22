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
 * Imports OHLCV (Open, High, Low, Close, Volume) data from CSV files
 * and generates synthetic orders around the real price action.
 * <p>
 * This allows using real price history while maintaining controllable
 * order flow characteristics.
 * </p>
 */
public class OHLCVCSVImporter implements MarketDataImporter {
    private final String csvFilePath;
    private final long seed;
    private final double ordersPerBar; // average orders per OHLCV bar
    private final double priceRangePct; // percentage of bar range for order prices
    private final double buyRatio; // probability of buy order
    private final double marketOrderRatio; // probability of market order
    private final int minQuantity; // minimum order quantity
    private final int maxQuantity; // maximum order quantity

    /**
     * Creates an OHLCV CSV importer.
     *
     * @param csvFilePath path to the CSV file with OHLCV data
     * @param seed random seed for reproducible order generation
     * @param ordersPerBar average number of orders to generate per OHLCV bar
     * @param priceRangePct percentage of bar's high-low range to use for order prices (0.0-1.0)
     * @param buyRatio probability of generating a buy order (0.0-1.0)
     * @param marketOrderRatio probability of generating a market order (0.0-1.0)
     * @param minQuantity minimum order quantity
     * @param maxQuantity maximum order quantity
     */
    public OHLCVCSVImporter(String csvFilePath, long seed,
                            double ordersPerBar, double priceRangePct,
                            double buyRatio, double marketOrderRatio,
                            int minQuantity, int maxQuantity) {
        this.csvFilePath = csvFilePath;
        this.seed = seed;
        this.ordersPerBar = ordersPerBar;
        this.priceRangePct = priceRangePct;
        this.buyRatio = buyRatio;
        this.marketOrderRatio = marketOrderRatio;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
    }

    @Override
    public List<Order> importOrders() throws Exception {
        List<Order> orders = new ArrayList<>();
        long orderCounter = 0;
        Random random = new Random(seed);

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            boolean isHeader = true;
            long timestampNs = System.nanoTime(); // starting timestamp

            while ((line = br.readLine()) != null) {
                // Skip empty lines
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }

                // Handle potential header line
                if (isHeader && (line.toLowerCase().contains("date") ||
                                 line.toLowerCase().contains("timestamp") ||
                                 line.toLowerCase().contains("open"))) {
                    isHeader = false;
                    continue;
                }

                // Parse CSV line: expecting format: timestamp,open,high,low,close,volume
                // or: date,time,open,high,low,close,volume
                String[] values = line.split(",");
                if (values.length < 5) {
                    continue; // Skip malformed lines
                }

                try {
                    // Parse OHLCV values (adjust indices based on actual format)
                    double open = Double.parseDouble(values[values.length - 5]);
                    double high = Double.parseDouble(values[values.length - 4]);
                    double low = Double.parseDouble(values[values.length - 3]);
                    double close = Double.parseDouble(values[values.length - 2]);
                    double volume = Double.parseDouble(values[values.length - 1]);

                    // Calculate price range for this bar
                    double priceRange = high - low;
                    if (priceRange <= 0) {
                        priceRange = close * 0.001; // fallback to 0.1% of price
                    }

                    // Determine number of orders for this bar (Poisson-like)
                    int ordersThisBar = PoissonSampler.sample(ordersPerBar, random);

                    // Generate orders for this bar
                    for (int i = 0; i < ordersThisBar; i++) {
                        // Generate order price within the bar's range, biased toward close
                        double priceOffset = random.nextDouble() * priceRange * priceRangePct;
                        double price = low + priceOffset + (close - (high + low) / 2) * (1 - priceRangePct);

                        // Ensure price is within bar bounds with some flexibility
                        price = Math.max(low * 0.99, Math.min(high * 1.01, price));

                        boolean buySide = random.nextDouble() < buyRatio;
                        boolean isMarket = random.nextDouble() < marketOrderRatio;
                        int quantity = minQuantity + random.nextInt(maxQuantity - minQuantity + 1);

                        Order order = new Order(
                                "REAL_" + orderCounter++,
                                buySide,
                                (long) Math.round(price),
                                quantity,
                                timestampNs + i * 1000000, // stagger timestamps within bar
                                isMarket ? OrderType.MARKET : OrderType.LIMIT
                        );

                        orders.add(order);
                    }

                    // Advance timestamp for next bar (approximate)
                    timestampNs += 60 * 1_000_000_000L; // 1 minute per bar (adjust as needed)

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
        return "OHLCV CSV file: " + csvFilePath;
    }
}

/**
 * Simple Poisson sampler for generating integer counts from a mean.
 */
class PoissonSampler {
    /**
     * Samples from a Poisson distribution with the given mean.
     *
     * @param mean  the mean (lambda) of the Poisson distribution
     * @param random random number generator
     * @return a sample from the Poisson distribution
     */
    public static int sample(double mean, Random random) {
        if (mean <= 0) {
            return 0;
        }

        double L = Math.exp(-mean);
        int k = 0;
        double p = 1.0;

        do {
            k++;
            p *= random.nextDouble();
        } while (p > L);

        return k - 1;
    }
}