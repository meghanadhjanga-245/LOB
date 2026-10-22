package com.lobsimulator.service.dto;

import java.util.List;

/**
 * Data Transfer Object for order responses.
 */
public class OrderResponse {
    private String orderId;
    private List<TradeResponse> trades;

    public OrderResponse() {}

    public OrderResponse(String orderId, List<TradeResponse> trades) {
        this.orderId = orderId;
        this.trades = trades;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<TradeResponse> getTrades() {
        return trades;
    }

    public void setTrades(List<TradeResponse> trades) {
        this.trades = trades;
    }
}