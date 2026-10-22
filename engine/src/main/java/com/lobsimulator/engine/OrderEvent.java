package com.lobsimulator.engine;

/**
 * Event object for the LMAX Disruptor Ring Buffer.
 */
public class OrderEvent {
    private boolean buySide;
    private long price;
    private int quantity;
    private boolean isMarket;
    private String orderId;

    public void set(boolean buySide, long price, int quantity, boolean isMarket, String orderId) {
        this.buySide = buySide;
        this.price = price;
        this.quantity = quantity;
        this.isMarket = isMarket;
        this.orderId = orderId;
    }

    public boolean isBuySide() { return buySide; }
    public long getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public boolean isMarket() { return isMarket; }
    public String getOrderId() { return orderId; }
}
