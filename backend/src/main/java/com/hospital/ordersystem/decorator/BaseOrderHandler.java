package com.hospital.ordersystem.decorator;

import com.hospital.ordersystem.model.Order;

public class BaseOrderHandler implements OrderHandler {

    @Override
    public Order handle(Order order) {
        return order;
    }
}
