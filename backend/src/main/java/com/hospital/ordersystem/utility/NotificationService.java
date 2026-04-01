package com.hospital.ordersystem.utility;

import com.hospital.ordersystem.model.Order;

public interface NotificationService {
    void notify(Order order, String event);
}
