package com.lobsimulator.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time order book updates.
 * <p>
 * Configures a message broker for handling WebSocket connections
 * and enables STOMP endpoints for client communication.
 * </p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures STOMP endpoints for clients to connect to.
     *
     * @param registry the endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Enable WebSocket connection with SockJS fallback at /ws endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Allow connections from any origin (adjust for production)
                .withSockJS();
    }

    /**
     * Configures the message broker for handling messages.
     *
     * @param registry the message broker registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple message broker for /topic destination prefix
        registry.enableSimpleBroker("/topic");
        // Set application destination prefix to /app
        registry.setApplicationDestinationPrefixes("/app");
    }
}