package com.hospital.ordersystem.decorator;

import com.hospital.ordersystem.model.Order;

/**
 * Decorator that validates required fields before passing the order
 * down the processing chain.  Layer: Business Logic.
 */
public class ValidationDecorator extends OrderHandlerDecorator {

    public ValidationDecorator(OrderHandler wrapped) {
        super(wrapped);
    }

    @Override
    public Order handle(Order order) {
        if (order.getPatientName() == null || order.getPatientName().isBlank()) {
            throw new IllegalArgumentException("Patient name is required.");
        }
        if (order.getClinician() == null || order.getClinician().isBlank()) {
            throw new IllegalArgumentException("Clinician name is required.");
        }
        if (order.getDescription() == null || order.getDescription().isBlank()) {
            throw new IllegalArgumentException("Order description is required.");
        }
        return wrapped.handle(order);
    }
}
