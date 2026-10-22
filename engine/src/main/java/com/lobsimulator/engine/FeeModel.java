package com.lobsimulator.engine;

/**
 * Maker-taker fee model for the matching engine.
 * <p>
 * Implements a tiered fee structure based on 30-day trading volume,
 * with maker rebates and taker fees.
 * </p>
 */
public class FeeModel {
    // Fee tiers (in basis points) - simplified example
    // In reality, these would be configurable and based on actual volume
    private static final double[] MAKER_FEES_BP = {-0.5, -0.4, -0.3, -0.2, -0.1, 0.0}; // Negative = rebate
    private static final double[] TAKER_FEES_BP = {0.5, 0.4, 0.3, 0.2, 0.1, 0.0};
    private static final long[] VOLUME_THRESHOLDS = {
        0L,           // Tier 1: 0 - 1M
        1_000_000L,   // Tier 2: 1M - 5M
        5_000_000L,   // Tier 3: 5M - 10M
        10_000_000L,  // Tier 4: 10M - 50M
        50_000_000L,  // Tier 5: 50M - 100M
        100_000_000L  // Tier 6: 100M+
    };

    private long volume30Day; // 30-day trading volume in ticks*quantity

    /**
     * Creates a fee model with zero initial volume.
     */
    public FeeModel() {
        this.volume30Day = 0L;
    }

    /**
     * Creates a fee model with the given 30-day volume.
     *
     * @param volume30Day 30-day trading volume
     */
    public FeeModel(long volume30Day) {
        this.volume30Day = volume30Day;
    }

    /**
     * Calculates the maker fee for a trade.
     *
     * @param price  trade price in ticks
     * @param quantity trade quantity
     * @return fee amount in ticks*quantity (can be negative for rebates)
     */
    public long calculateMakerFee(long price, int quantity) {
        long notional = price * quantity;
        int tier = getFeeTier(volume30Day);
        double feeBp = MAKER_FEES_BP[tier];
        return Math.round(notional * feeBp / 10000.0);
    }

    /**
     * Calculates the taker fee for a trade.
     *
     * @param price  trade price in ticks
     * @param quantity trade quantity
     * @return fee amount in ticks*quantity
     */
    public long calculateTakerFee(long price, int quantity) {
        long notional = price * quantity;
        int tier = getFeeTier(volume30Day);
        double feeBp = TAKER_FEES_BP[tier];
        return Math.round(notional * feeBp / 10000.0);
    }

    /**
     * Updates the 30-day trading volume with the notional value of a trade.
     * <p>
     * In a real implementation, this would use a rolling window.
     * For simplicity, we just accumulate.
     * </p>
     *
     * @param price  trade price in ticks
     * @param quantity trade quantity
     */
    public void addVolume(long price, int quantity) {
        volume30Day += price * quantity;
    }

    /**
     * Gets the fee tier based on 30-day volume.
     *
     * @param volume30Day 30-day trading volume
     * @return fee tier index (0-5)
     */
    private int getFeeTier(long volume30Day) {
        if (volume30Day >= VOLUME_THRESHOLDS[5]) return 5;
        if (volume30Day >= VOLUME_THRESHOLDS[4]) return 4;
        if (volume30Day >= VOLUME_THRESHOLDS[3]) return 3;
        if (volume30Day >= VOLUME_THRESHOLDS[2]) return 2;
        if (volume30Day >= VOLUME_THRESHOLDS[1]) return 1;
        return 0;
    }

    /**
     * Gets the 30-day trading volume.
     *
     * @return 30-day trading volume in ticks*quantity
     */
    public long getVolume30Day() {
        return volume30Day;
    }

    /**
     * Sets the 30-day trading volume.
     *
     * @param volume30Day 30-day trading volume
     */
    public void setVolume30Day(long volume30Day) {
        this.volume30Day = volume30Day;
    }
}