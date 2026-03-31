package com.hospital.ordersystem.strategy;

import com.hospital.ordersystem.model.Order;

import java.util.List;

/**
 * Strategy pattern — encapsulates interchangeable triage algorithms behind
 * a common interface so the active policy can be swapped at runtime without
 * modifying TriagingEngine or OrderManager.  Layer: Business Logic.
 */
public interface TriageStrategy {
    List<Order> sort(List<Order> orders);
}
