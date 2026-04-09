package com.hospital.ordersystem.strategy;

import com.hospital.ordersystem.model.Order;

import java.util.Comparator;
import java.util.List;

public class LoadBalancingTriageStrategy implements TriageStrategy {

    @Override
    public List<Order> sort(List<Order> orders) {
        return orders.stream()
                .sorted(Comparator.comparing(Order::getSubmittedAt))
                .toList();
    }
}
