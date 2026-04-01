package com.hospital.ordersystem.strategy;

import com.hospital.ordersystem.model.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

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
