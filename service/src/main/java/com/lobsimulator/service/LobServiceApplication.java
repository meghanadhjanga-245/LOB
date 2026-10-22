package com.lobsimulator.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the LOB Service (Backend API).
 * <p>
 * This is a Spring Boot application that provides REST endpoints and WebSocket
 * interfaces for interacting with the limit order book.
 * </p>
 */
@SpringBootApplication
public class LobServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LobServiceApplication.class, args);
    }
}