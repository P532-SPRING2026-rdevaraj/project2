package com.hospital.ordersystem.decorator;

import com.hospital.ordersystem.model.Order;

import java.time.LocalDateTime;

/**
 * Decorator that logs an audit entry after the order passes through
 * the processing chain.  Layer: Business Logic.
 */
public class AuditLoggingDecorator extends OrderHandlerDecorator {

    public AuditLoggingDecorator(OrderHandler wrapped) {
        super(wrapped);
    }

    @Override
    public Order handle(Order order) {
        Order processed = wrapped.handle(order);
        System.out.printf("[AUDIT] %s | Order %s (%s) accepted for processing | Patient: %s%n",
                LocalDateTime.now(),
                processed.getOrderId(),
                processed.getType(),
                processed.getPatientName());
        return processed;
    }
}
