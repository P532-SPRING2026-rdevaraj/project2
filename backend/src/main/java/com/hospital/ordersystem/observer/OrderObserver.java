package com.hospital.ordersystem.observer;

import com.hospital.ordersystem.model.Order;

public interface OrderObserver {
    void onOrderEvent(Order order, String event);
}
