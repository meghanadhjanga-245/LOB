package com.lobsimulator.engine;

/**
 * Represents a price level in the order book, containing a FIFO list of orders at the same price.
 * Optimized to maintain total quantity in O(1).
 */
public class PriceLevel {
    private final long price;
    private Order head;
    private Order tail;
    private int size; // number of orders at this level
    private int totalQuantity; // cached total quantity of all orders at this level

    public PriceLevel(long price) {
        this.price = price;
        this.head = null;
        this.tail = null;
        this.size = 0;
        this.totalQuantity = 0;
    }

    public long getPrice() {
        return price;
    }

    public int getSize() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public Order getFirstOrder() {
        return head;
    }

    public void addOrder(Order order) {
        if (order.getPrev() != null || order.getNext() != null) {
            throw new IllegalStateException("Order is already in a list");
        }
        if (head == null) {
            head = tail = order;
        } else {
            tail.setNext(order);
            order.setPrev(tail);
            tail = order;
        }
        size++;
        totalQuantity += order.getQuantity();
    }

    public void removeOrder(Order order) {
        if (order.getPrev() == null) {
            head = order.getNext();
        } else {
            order.getPrev().setNext(order.getNext());
        }
        if (order.getNext() == null) {
            tail = order.getPrev();
        } else {
            order.getNext().setPrev(order.getPrev());
        }
        order.setPrev(null);
        order.setNext(null);
        size--;
        totalQuantity -= order.getQuantity();
    }
    
    public void updateOrderQuantity(Order order, int delta) {
        this.totalQuantity += delta;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }
}
