package com.lobsimulator.engine;
import java.util.Objects;
public class Order {
    private String orderId; 
    private final boolean buySide; 
    private long price; 
    private int quantity; 
    private int hiddenQuantity; 
    private long timestamp; 
    private OrderType type; 
    private boolean isIceberg;
    private int peakSize;
    private Order prev;
    private Order next;
    private long makerFeeRebate;
    private long takerFeePaid;
    public Order(String orderId, boolean buySide, long price, int quantity, long timestamp, OrderType type) {
        this.orderId = orderId;
        this.buySide = buySide;
        this.price = price;
        this.quantity = quantity;
        this.hiddenQuantity = 0;
        this.timestamp = timestamp;
        this.type = type;
        this.prev = null;
        this.next = null;
        this.makerFeeRebate = 0;
        this.takerFeePaid = 0;
        this.isIceberg = false;
    }
    public void reset(String orderId, long price, int quantity, long timestamp, OrderType type) {
        this.orderId = orderId;
        this.price = price;
        this.quantity = quantity;
        this.hiddenQuantity = 0;
        this.timestamp = timestamp;
        this.type = type;
        this.prev = null;
        this.next = null;
        this.makerFeeRebate = 0;
        this.takerFeePaid = 0;
        this.isIceberg = false;
    }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public boolean isBuySide() { return buySide; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getHiddenQuantity() { return hiddenQuantity; }
    public void setHiddenQuantity(int hiddenQuantity) { this.hiddenQuantity = hiddenQuantity; }
    public int getTotalQuantity() { return quantity + hiddenQuantity; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public OrderType getType() { return type; }
    public void setType(OrderType type) { this.type = type; }
    public boolean isMarket() { return type == OrderType.MARKET; }
    public Order getPrev() { return prev; }
    public void setPrev(Order prev) { this.prev = prev; }
    public Order getNext() { return next; }
    public void setNext(Order next) { this.next = next; }
    public boolean isIceberg() { return isIceberg; }
    public int getPeakSize() { return peakSize; }
    public void setIceberg(boolean iceberg, int peakSize) {
        this.isIceberg = iceberg;
        this.peakSize = peakSize;
        int total = getTotalQuantity();
        if (iceberg && peakSize > 0) {
            this.quantity = Math.min(total, peakSize);
            this.hiddenQuantity = total - this.quantity;
        }
    }
    public void refillIceberg() {
        if (!isIceberg || hiddenQuantity <= 0) return;
        int refill = Math.min(hiddenQuantity, peakSize);
        this.quantity += refill;
        this.hiddenQuantity -= refill;
    }
    public long getMakerFeeRebate() { return makerFeeRebate; }
    public void addMakerFeeRebate(long amount) { this.makerFeeRebate += amount; }
    public long getTakerFeePaid() { return takerFeePaid; }
    public void addTakerFeePaid(long amount) { this.takerFeePaid += amount; }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }
    @Override
    public int hashCode() { return Objects.hash(orderId); }
}
