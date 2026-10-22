package com.lobsimulator.service.dto;

/**
 * Data Transfer Object for order requests.
 */
public class OrderRequest {
    private String orderId;
    private boolean buySide; // true for BUY, false for SELL
    private long price; // in ticks
    private int quantity;
    private long timestamp; // time of creation
    private boolean isMarket; // true for market order, false for limit
    private String orderType; // LIMIT, MARKET, IOC, FOK, ICEBERG
    private int peakSize; // for iceberg orders

    // Constructors
    public OrderRequest() {}

    public OrderRequest(String orderId, boolean buySide, long price, int quantity,
                       long timestamp, boolean isMarket) {
        this.orderId = orderId;
        this.buySide = buySide;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
        this.isMarket = isMarket;
    }

    public OrderRequest(String orderId, boolean buySide, long price, int quantity,
                       long timestamp, boolean isMarket, String orderType, int peakSize) {
        this.orderId = orderId;
        this.buySide = buySide;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
        this.isMarket = isMarket;
        this.orderType = orderType;
        this.peakSize = peakSize;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public boolean isBuySide() {
        return buySide;
    }

    public void setBuySide(boolean buySide) {
        this.buySide = buySide;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isMarket() {
        return isMarket;
    }

    public void setMarket(boolean market) {
        isMarket = market;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public int getPeakSize() {
        return peakSize;
    }

    public void setPeakSize(int peakSize) {
        this.peakSize = peakSize;
    }
}