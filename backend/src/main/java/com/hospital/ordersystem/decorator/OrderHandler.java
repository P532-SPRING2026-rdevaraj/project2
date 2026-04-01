package com.hospital.ordersystem.decorator;

import com.hospital.ordersystem.model.Order;

public interface OrderHandler {
    Order handle(Order order);
}
