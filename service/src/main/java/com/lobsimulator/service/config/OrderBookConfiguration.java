package com.lobsimulator.service.config;

import com.lobsimulator.engine.MatchingEngine;
import com.lobsimulator.engine.OrderBook;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderBookConfiguration {

    @Bean
    public OrderBook orderBook() {
        return new OrderBook();
    }

    /**
     * FIX: MatchingEngine was missing as a Spring bean.
     * OrderBookController injects both OrderBook and MatchingEngine via constructor.
     * Without this bean, Spring throws UnsatisfiedDependencyException at startup.
     */
    @Bean
    public MatchingEngine matchingEngine(OrderBook orderBook) {
        return new MatchingEngine(orderBook);
    }
}