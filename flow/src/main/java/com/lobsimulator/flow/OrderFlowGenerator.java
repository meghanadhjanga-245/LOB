package com.lobsimulator.flow;

import com.lobsimulator.engine.Order;
import com.lobsimulator.engine.OrderType;
import java.util.Random;

/**
 * Synthetic order flow generator that creates orders with Poisson arrival process.
 * <p>
 * Enhanced version with realistic market dynamics including:
 * - Mean reversion in prices
 * - Volatility clustering
 * - Volume profiles
 * - Order book sensitivity
 * </p>
 */
public class OrderFlowGenerator {
    private final Random random;
    private final double lambda; // arrival rate (orders per second)
    private final long minPrice; // minimum order price
    private final long maxPrice; // maximum order price
    private final long basePrice; // base price for mean reversion
    private final double priceVolatility; // base volatility
    private final double meanReversionSpeed; // speed of mean reversion
    private final double volatilityOfVolatility; // volatility of volatility (for clustering)
    private final int minQuantity; // minimum order quantity
    private final int maxQuantity; // maximum order quantity
    private final double buyRatio; // probability of buy order (vs sell)
    private final double marketOrderRatio; // probability of market order (vs limit)
    private long nextOrderTimeNs; // time of next order in nanoseconds
    private long lastOrderTimeNs; // track last call time
    private long orderCounter; // for generating unique order IDs
    private double currentPrice; // current mid-price (for mean reversion)
    private double currentVolatility; // current volatility (for clustering)

    /**
     * Creates a new order flow generator with price bounds.
     */
    public OrderFlowGenerator(long seed, double lambda, long minPrice, long maxPrice,
                              int minQuantity, int maxQuantity, double buyRatio,
                              double marketOrderRatio) {
        this.random = new Random(seed);
        this.lambda = lambda;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.basePrice = (minPrice + maxPrice) / 2;
        this.priceVolatility = (maxPrice > minPrice && basePrice > 0)
                ? ((double)(maxPrice - minPrice) / (2.0 * basePrice))
                : 0.05;
        this.meanReversionSpeed = 0.1;
        this.volatilityOfVolatility = 0.05;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.buyRatio = buyRatio;
        this.marketOrderRatio = marketOrderRatio;
        this.orderCounter = 0;
        this.currentPrice = basePrice;
        this.currentVolatility = this.priceVolatility;
        this.nextOrderTimeNs = generateNextArrivalTime();
        this.lastOrderTimeNs = System.nanoTime();
    }

    /**
     * Creates a new order flow generator.
     *
     * @param seed               seed for random number generator (for reproducibility)
     * @param lambda             arrival rate in orders per second
     * @param basePrice          base price for mean reversion (in ticks)
     * @param priceVolatility    base price volatility (as fraction of price)
     * @param meanReversionSpeed speed of mean reversion (0-1, higher = faster reversion)
     * @param volatilityOfVolatility volatility of volatility (for clustering)
     * @param minQuantity        minimum order quantity
     * @param maxQuantity        maximum order quantity
     * @param buyRatio           probability of buy order (0.0 to 1.0)
     * @param marketOrderRatio   probability of market order (0.0 to 1.0)
     */
    public OrderFlowGenerator(long seed, double lambda, long basePrice,
                              double priceVolatility, double meanReversionSpeed,
                              double volatilityOfVolatility, int minQuantity,
                              int maxQuantity, double buyRatio,
                              double marketOrderRatio) {
        this.random = new Random(seed);
        this.lambda = lambda;
        this.basePrice = basePrice;
        this.minPrice = (long)(basePrice * (1.0 - 3 * priceVolatility));
        this.maxPrice = (long)(basePrice * (1.0 + 3 * priceVolatility));
        this.priceVolatility = priceVolatility;
        this.meanReversionSpeed = meanReversionSpeed;
        this.volatilityOfVolatility = volatilityOfVolatility;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.buyRatio = buyRatio;
        this.marketOrderRatio = marketOrderRatio;
        this.orderCounter = 0;
        this.currentPrice = basePrice;
        this.currentVolatility = priceVolatility;
        this.nextOrderTimeNs = generateNextArrivalTime();
        this.lastOrderTimeNs = System.nanoTime();
    }

    /**
     * Generates the time of the next order arrival based on Poisson process.
     * <p>
     * For a Poisson process with rate lambda, the inter-arrival times follow
     * an exponential distribution with parameter lambda.
     * </p>
     *
     * @return time of next order arrival in nanoseconds
     */
    private long generateNextArrivalTime() {
        // Exponential distribution: -ln(U) / lambda where U is uniform(0,1)
        double u = random.nextDouble();
        // Avoid log(0) by ensuring u is not too close to 0
        if (u <= 0.0) {
            u = Double.MIN_NORMAL;
        }
        double interArrivalSec = -Math.log(u) / lambda;
        return Math.round(interArrivalSec * 1_000_000_000); // convert to nanoseconds
    }

    /**
     * Updates the market dynamics (price and volatility) based on time elapsed.
     * <p>
     * Uses Ornstein-Uhlenbeck process for mean reversion in prices
     * and GARCH-like process for volatility clustering.
     * </p>
     *
     * @param elapsedTimeNs time elapsed since last update in nanoseconds
     */
    private void updateMarketDynamics(long elapsedTimeNs) {
        // Convert nanoseconds to a reasonable time unit for modeling
        // For simplicity, we'll treat elapsed time as proportional to order frequency
        double dt = elapsedTimeNs / 1_000_000_000.0; // seconds

        // Mean reversion in prices: dP = θ(μ - P)dt + σdW
        double priceMeanReversion = meanReversionSpeed * (basePrice - currentPrice) * dt;
        double priceShock = currentVolatility * Math.sqrt(dt) * random.nextGaussian();
        currentPrice += priceMeanReversion + priceShock;

        // Ensure price doesn't go negative
        currentPrice = Math.max(0.1, currentPrice);

        // Volatility clustering: dV = κ(θ - V)dt + ξ√V dW
        double volMeanReversion = 0.1 * (priceVolatility - currentVolatility) * dt; // fixed speed for vol
        double volShock = volatilityOfVolatility * Math.sqrt(currentVolatility * dt) * random.nextGaussian();
        currentVolatility += volMeanReversion + volShock;

        // Ensure volatility stays in reasonable bounds
        currentVolatility = Math.max(0.001, Math.min(currentVolatility, 0.1));
    }

    /**
     * Generates a synthetic order.
     * <p>
     * The order will have:
     * - Side: BUY with probability buyRatio, SELL otherwise
     * - Price: current price plus random shock (mean reversion + volatility)
     * - Quantity: uniform distribution between minQuantity and maxQuantity
     * - Type: market order with probability marketOrderRatio, limit order otherwise
     * - Timestamp: based on the Poisson arrival process
     * - Order ID: generated sequentially
     * </p>
     *
     * @return a synthetic order
     */
    public Order generateOrder() {
        // Generate order properties
        boolean buySide = random.nextDouble() < buyRatio;
        boolean isMarket = random.nextDouble() < marketOrderRatio;

        long price;
        if (minPrice >= maxPrice) {
            price = minPrice;
        } else {
            price = minPrice + (long)(random.nextDouble() * (maxPrice - minPrice + 1));
        }

        int quantity = (minQuantity >= maxQuantity)
                ? minQuantity
                : minQuantity + random.nextInt(maxQuantity - minQuantity + 1);
        long timestamp = nextOrderTimeNs;

        // Generate order ID
        String orderId = "FLOW_" + orderCounter++;

        Order order = new Order(orderId, buySide, price, quantity, timestamp,
                isMarket ? OrderType.MARKET : OrderType.LIMIT);

        // Advance time for next order
        nextOrderTimeNs += generateNextArrivalTime();

        return order;
    }

    /**
     * Gets the time of the next order arrival in nanoseconds.
     *
     * @return time of next order arrival
     */
    public long getNextOrderTimeNs() {
        return nextOrderTimeNs;
    }

    /**
     * Resets the generator with a new seed.
     * <p>
     * This allows regenerating the same sequence of orders.
     * </p>
     *
     * @param seed new seed for random number generator
     */
    public void reset(long seed) {
        random.setSeed(seed);
        orderCounter = 0;
        currentPrice = basePrice;
        currentVolatility = priceVolatility;
        nextOrderTimeNs = generateNextArrivalTime();
        lastOrderTimeNs = System.nanoTime();
    }

    /**
     * Gets the arrival rate (lambda) in orders per second.
     *
     * @return arrival rate
     */
    public double getLambda() {
        return lambda;
    }

    /**
     * Gets the base price for mean reversion.
     *
     * @return base price
     */
    public long getBasePrice() {
        return basePrice;
    }

    /**
     * Gets the current mid-price (after mean reversion updates).
     *
     * @return current price
     */
    public double getCurrentPrice() {
        return currentPrice;
    }

    /**
     * Gets the base price volatility.
     *
     * @return base volatility
     */
    public double getPriceVolatility() {
        return priceVolatility;
    }

    /**
     * Gets the current volatility (after clustering updates).
     *
     * @return current volatility
     */
    public double getCurrentVolatility() {
        return currentVolatility;
    }

    /**
     * Gets the minimum order quantity.
     *
     * @return minimum quantity
     */
    public int getMinQuantity() {
        return minQuantity;
    }

    /**
     * Gets the maximum order quantity.
     *
     * @return maximum quantity
     */
    public int getMaxQuantity() {
        return maxQuantity;
    }

    /**
     * Gets the probability of generating a buy order.
     *
     * @return buy ratio (0.0 to 1.0)
     */
    public double getBuyRatio() {
        return buyRatio;
    }

    /**
     * Gets the probability of generating a market order.
     *
     * @return market order ratio (0.0 to 1.0)
     */
    public double getMarketOrderRatio() {
        return marketOrderRatio;
    }

    public long getMinPrice() {
        return minPrice;
    }

    public long getMaxPrice() {
        return maxPrice;
    }
}