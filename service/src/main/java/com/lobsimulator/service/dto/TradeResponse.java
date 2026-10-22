package com.lobsimulator.service.dto;

/**
 * Data Transfer Object for trade responses.
 */
public class TradeResponse {
    private long price; // trade price in ticks
    private int quantity; // trade quantity
    private boolean aggressorBuySide; // true if aggressor order is BUY, false for SELL
    private String makerOrderId; // resting order that provided liquidity
    private String takerOrderId; // aggressive order that took liquidity
    private long timestamp; // timestamp of the trade
    private long makerFee;
    private long takerFee;
    private double slippage;
    private double vwap;

    public TradeResponse() {}

    public TradeResponse(long price, int quantity, boolean aggressorBuySide,
                        String makerOrderId, String takerOrderId, long timestamp,
                        long makerFee, long takerFee, double slippage, double vwap) {
        this.price = price;
        this.quantity = quantity;
        this.aggressorBuySide = aggressorBuySide;
        this.makerOrderId = makerOrderId;
        this.takerOrderId = takerOrderId;
        this.timestamp = timestamp;
        this.makerFee = makerFee;
        this.takerFee = takerFee;
        this.slippage = slippage;
        this.vwap = vwap;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isAggressorBuySide() {
        return aggressorBuySide;
    }

    public void setAggressorBuySide(boolean aggressorBuySide) {
        this.aggressorBuySide = aggressorBuySide;
    }

    public String getMakerOrderId() {
        return makerOrderId;
    }

    public void setMakerOrderId(String makerOrderId) {
        this.makerOrderId = makerOrderId;
    }

    public String getTakerOrderId() {
        return takerOrderId;
    }

    public void setTakerOrderId(String takerOrderId) {
        this.takerOrderId = takerOrderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getMakerFee() { return makerFee; }
    public void setMakerFee(long makerFee) { this.makerFee = makerFee; }

    public long getTakerFee() { return takerFee; }
    public void setTakerFee(long takerFee) { this.takerFee = takerFee; }

    public double getSlippage() { return slippage; }
    public void setSlippage(double slippage) { this.slippage = slippage; }

    public double getVwap() { return vwap; }
    public void setVwap(double vwap) { this.vwap = vwap; }
}