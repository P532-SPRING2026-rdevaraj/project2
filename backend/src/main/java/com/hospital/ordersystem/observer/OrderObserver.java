package com.hospital.ordersystem.observer;

import com.hospital.ordersystem.model.Order;

/**
 * Observer pattern — decouples internal state changes (order status updates)
 * from the components that react to them (notification subsystem, dashboard).
 * Layer: Business Logic.
 */
public interface OrderObserver {
    void onOrderEvent(Order order, String event);
}
