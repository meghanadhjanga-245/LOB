package com.lobsimulator.engine;

public class TradeEvent {
    private long price;
    private int qty;
    private boolean buySide;
    private String makerId;
    private String takerId;
    private long timestamp;
    private long makerFee;
    private long takerFee;
    private double slippage; 
    private double vwap;    

    public TradeEvent(long price, int qty, boolean buySide, String makerId, String takerId, long timestamp) {
        this.price = price;
        this.qty = qty;
        this.buySide = buySide;
        this.makerId = makerId;
        this.takerId = takerId;
        this.timestamp = timestamp;
    }

    public void reset(long price, int qty, boolean buySide, String makerId, String takerId, long timestamp) {
        this.price = price;
        this.qty = qty;
        this.buySide = buySide;
        this.makerId = makerId;
        this.takerId = takerId;
        this.timestamp = timestamp;
        this.makerFee = 0;
        this.takerFee = 0;
        this.slippage = 0;
        this.vwap = 0;
    }

    public long getPrice() { return price; }
    public int getQty() { return qty; }
    public int getQuantity() { return qty; }
    public boolean isBuySide() { return buySide; }
    public boolean isAggressorBuySide() { return buySide; }
    public String getMakerId() { return makerId; }
    public String getTakerId() { return takerId; }
    public long getTimestamp() { return timestamp; }
    public long getMakerFee() { return makerFee; }
    public void setMakerFee(long makerFee) { this.makerFee = makerFee; }
    public long getTakerFee() { return takerFee; }
    public void setTakerFee(long takerFee) { this.takerFee = takerFee; }
    public double getSlippage() { return slippage; }
    public void setSlippage(double slippage) { this.slippage = slippage; }
    public double getVwap() { return vwap; }
    public void setVwap(double vwap) { this.vwap = vwap; }
}
