package com.hospital.ordersystem.observer;

import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.utility.NotificationService;
import org.springframework.stereotype.Component;

@Component
public class NotificationObserver implements OrderObserver {

    private final NotificationService notificationService;

    public NotificationObserver(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onOrderEvent(Order order, String event) {
        notificationService.notify(order, event);
    }
}
