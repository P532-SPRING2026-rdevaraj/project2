package com.hospital.ordersystem.strategy;

import com.hospital.ordersystem.model.Order;

import java.util.Comparator;
import java.util.List;

/**
 * Load-balancing triage: orders are sorted FIFO (submission time) so they
 * are distributed evenly across fulfilment staff regardless of priority.
 * Staff members with lighter loads will naturally claim orders sooner,
 * achieving even distribution without pre-assignment.
 */
public class LoadBalancingTriageStrategy implements TriageStrategy {

    @Override
    public List<Order> sort(List<Order> orders) {
        return orders.stream()
                .sorted(Comparator.comparing(Order::getSubmittedAt))
                .toList();
    }
}
