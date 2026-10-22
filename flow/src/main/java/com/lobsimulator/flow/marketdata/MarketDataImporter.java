package com.lobsimulator.flow.marketdata;

import com.lobsimulator.engine.Order;
import java.util.List;

/**
 * Interface for importing real market data and converting it to simulator orders.
 * <p>
 * Implementations can read from various sources (CSV, JSON, APIs, etc.)
 * and convert the data to Order objects that can be processed by the MatchingEngine.
 * </p>
 */
public interface MarketDataImporter {
    /**
     * Imports market data and converts it to a list of orders.
     *
     * @return list of orders representing the market data
     * @throws Exception if data import or conversion fails
     */
    List<Order> importOrders() throws Exception;

    /**
     * Gets a description of the data source.
     *
     * @return description of the market data source
     */
    String getSourceDescription();
}