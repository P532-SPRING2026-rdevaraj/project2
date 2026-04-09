package com.hospital.ordersystem.utility;

import com.hospital.ordersystem.model.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InAppNotificationService implements NotificationService {

    private final AtomicInteger badgeCount = new AtomicInteger(0);

    @Override
    public void notify(Order order, String event) {
        int count = badgeCount.incrementAndGet();
        System.out.printf("[IN-APP] Badge count → %d | Event: %s | Order: %s%n",
                count, event, order.getOrderId());
    }

    public int getBadgeCount() {
        return badgeCount.get();
    }

    public void resetBadgeCount() {
        badgeCount.set(0);
    }
}
