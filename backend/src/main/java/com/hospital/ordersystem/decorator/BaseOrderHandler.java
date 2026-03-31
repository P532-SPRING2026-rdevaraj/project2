package com.hospital.ordersystem.decorator;

import com.hospital.ordersystem.model.Order;

/**
 * Concrete base component — passes the order through unchanged.
 * All decorators wrap this.  Layer: Business Logic.
 */
public class BaseOrderHandler implements OrderHandler {

    @Override
    public Order handle(Order order) {
        return order;
    }
}
