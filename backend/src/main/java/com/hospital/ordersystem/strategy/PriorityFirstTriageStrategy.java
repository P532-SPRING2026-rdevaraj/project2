package com.hospital.ordersystem.strategy;

import com.hospital.ordersystem.model.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Week 1 triage: STAT first, then URGENT, then ROUTINE.
 * Ties are broken by submission time (FIFO).
 */
@Component
public class PriorityFirstTriageStrategy implements TriageStrategy {

    @Override
    public List<Order> sort(List<Order> orders) {
        return orders.stream()
                .sorted(Comparator
                        .comparingInt((Order o) -> o.getPriority().getLevel()).reversed()
                        .thenComparing(Order::getSubmittedAt))
                .toList();
    }
}
