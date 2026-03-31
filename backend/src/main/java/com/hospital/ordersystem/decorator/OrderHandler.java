package com.hospital.ordersystem.decorator;

import com.hospital.ordersystem.model.Order;

/**
 * Decorator pattern — defines the component interface so processing steps
 * (validation, priority boosting, audit logging) can be stacked transparently
 * around a base handler without modifying it.  Layer: Business Logic.
 */
public interface OrderHandler {
    Order handle(Order order);
}
