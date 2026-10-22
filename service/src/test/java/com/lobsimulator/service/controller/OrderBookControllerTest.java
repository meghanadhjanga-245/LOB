package com.lobsimulator.service.controller;

import com.lobsimulator.engine.MatchingEngine;
import com.lobsimulator.engine.OrderBook;
import com.lobsimulator.service.dto.OrderRequest;
import com.lobsimulator.service.dto.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderBookController.
 */
class OrderBookControllerTest {

    @Mock
    private OrderBook orderBookMock;

    // FIX: MatchingEngine mock was missing — controller needs both beans injected
    @Mock
    private MatchingEngine matchingEngineMock;

    @InjectMocks
    private OrderBookController orderBookController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSubmitLimitOrderRequest_structureIsValid() {
        OrderRequest request = new OrderRequest(
                "ORDER_1",
                true,  // BUY
                100L,  // price
                10,    // quantity
                System.nanoTime(),
                false  // limit order
        );

        assertNotNull(request);
        assertTrue(request.isBuySide());
        assertEquals(100L, request.getPrice());
        assertEquals(10, request.getQuantity());
        assertFalse(request.isMarket());
    }

    @Test
    void testGetBestBid_returnsOkWhenBookEmpty() {
        when(orderBookMock.getBestBid()).thenReturn(null);
        ResponseEntity<long[]> response = orderBookController.getBestBid();
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testGetBestAsk_returnsOkWhenBookEmpty() {
        when(orderBookMock.getBestAsk()).thenReturn(null);
        ResponseEntity<long[]> response = orderBookController.getBestAsk();
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testGetSpread_returnsZeroWhenBookEmpty() {
        when(orderBookMock.getBestBid()).thenReturn(null);
        when(orderBookMock.getBestAsk()).thenReturn(null);
        ResponseEntity<Double> response = orderBookController.getSpread();
        assertEquals(200, response.getStatusCode().value());
        assertEquals(0.0, response.getBody());
    }

    @Test
    void testGetSpread_returnsCorrectSpread() {
        when(orderBookMock.getBestBid()).thenReturn(new long[]{100L, 10L});
        when(orderBookMock.getBestAsk()).thenReturn(new long[]{102L, 5L});
        ResponseEntity<Double> response = orderBookController.getSpread();
        assertEquals(200, response.getStatusCode().value());
        assertEquals(2.0, response.getBody());
    }

    @Test
    void testCancelOrder_callsCancelOnBook() {
        when(orderBookMock.cancelOrder("ORDER_1")).thenReturn(true);
        ResponseEntity<Boolean> response = orderBookController.cancelOrder("ORDER_1");
        verify(orderBookMock, times(1)).cancelOrder("ORDER_1");
        assertTrue(Boolean.TRUE.equals(response.getBody()));
    }

    @Test
    void testIsCrossed_returnsFalseForEmptyBook() {
        when(orderBookMock.isCrossed()).thenReturn(false);
        ResponseEntity<Boolean> response = orderBookController.isCrossed();
        assertFalse(Boolean.TRUE.equals(response.getBody()));
    }

    @Test
    void testCheckInvariants_returnsTrueForEmptyBook() {
        when(orderBookMock.checkInvariants()).thenReturn(true);
        ResponseEntity<Boolean> response = orderBookController.checkInvariants();
        assertTrue(Boolean.TRUE.equals(response.getBody()));
    }
}