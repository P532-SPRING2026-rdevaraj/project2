package com.hospital.ordersystem.utility;

import com.hospital.ordersystem.model.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EmailNotificationService implements NotificationService {

    @Override
    public void notify(Order order, String event) {
        System.out.printf("""
                [EMAIL] -------------------------------------------
                  To      : fulfilment-team@hospital.local
                  Subject : [%s] Order %s – %s (%s)
                  Body    : Patient %s | Clinician %s | Priority %s | Time %s
                [EMAIL] -------------------------------------------
                %n""",
                event, order.getOrderId(), order.getType(), order.getStatus(),
                order.getPatientName(), order.getClinician(),
                order.getPriority(), LocalDateTime.now());
    }
}
