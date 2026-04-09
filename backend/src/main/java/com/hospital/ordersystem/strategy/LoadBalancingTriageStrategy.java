package com.hospital.ordersystem.strategy;

import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LoadBalancingTriageStrategy implements TriageStrategy {

    private final OrderAccess orderAccess;

    public LoadBalancingTriageStrategy(OrderAccess orderAccess) {
        this.orderAccess = orderAccess;
    }

    @Override
    public List<Order> sort(List<Order> orders) {
        // Count IN_PROGRESS orders per staff member
        Map<String, Long> workload = orderAccess.listAllOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS
                          && o.getClaimedBy() != null)
                .collect(Collectors.groupingBy(Order::getClaimedBy, Collectors.counting()));

        // Sort pending orders by how many in-progress orders the last
        // known handler of that order type has. Falls back to submission
        // time (FIFO) when workloads are equal or no staff data exists.
        return orders.stream()
                .sorted(Comparator
                        .comparingLong((Order o) -> workload.getOrDefault(o.getClinician(), 0L))
                        .thenComparing(Order::getSubmittedAt))
                .toList();
    }
}
