package com.hospital.ordersystem.utility;

import com.hospital.ordersystem.model.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ConsoleNotificationService implements NotificationService {

    @Override
    public void notify(Order order, String event) {
        System.out.printf("[NOTIFICATION] %s | Event: %-20s | Order: %s | Patient: %-20s | Clinician: %-15s | Priority: %s%n",
                LocalDateTime.now(), event, order.getOrderId(),
                order.getPatientName(), order.getClinician(), order.getPriority());
    }
}
