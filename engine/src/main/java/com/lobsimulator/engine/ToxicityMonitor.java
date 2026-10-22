package com.lobsimulator.engine;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors market toxicity using a simplified VPIN-like metric.
 * Tracks the imbalance between buy and sell volume in fixed buckets.
 */
public class ToxicityMonitor {
    private final int bucketSize;
    private final AtomicLong currentBuyVol = new AtomicLong(0);
    private final AtomicLong currentSellVol = new AtomicLong(0);
    private final AtomicLong totalVolume = new AtomicLong(0);
    
    private volatile double currentToxicity = 0.0;

    public ToxicityMonitor(int bucketSize) {
        this.bucketSize = bucketSize;
    }

    /**
     * Updates the monitor with a new trade.
     * @param qty quantity traded
     * @param buySide true if the aggressor was a buyer
     */
    public void onTrade(int qty, boolean buySide) {
        if (buySide) {
            currentBuyVol.addAndGet(qty);
        } else {
            currentSellVol.addAndGet(qty);
        }
        
        long vol = totalVolume.addAndGet(qty);
        
        // When bucket is full, calculate toxicity and reset
        if (vol >= bucketSize) {
            calculateToxicity();
            resetBucket();
        }
    }

    private void calculateToxicity() {
        long buy = currentBuyVol.get();
        long sell = currentSellVol.get();
        // Toxicity = |BuyVol - SellVol| / TotalVol
        this.currentToxicity = (double) Math.abs(buy - sell) / (buy + sell);
    }

    private void resetBucket() {
        currentBuyVol.set(0);
        currentSellVol.set(0);
        totalVolume.set(0);
    }

    public double getToxicity() {
        return currentToxicity;
    }

    public String getToxicityLevel() {
        if (currentToxicity < 0.2) return "LOW";
        if (currentToxicity < 0.5) return "MEDIUM";
        return "HIGH";
    }
}
