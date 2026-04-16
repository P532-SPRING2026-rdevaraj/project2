package com.hospital.ordersystem.strategy;

import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Load-balancing triage strategy.
 *
 * <p>Per the spec: "orders are distributed evenly across available fulfilment
 * staff members; the staff member with the fewest in-progress orders receives
 * the next order regardless of priority."
 *
 * <p>In a manual-claim system the queue sort cannot pre-assign orders to specific
 * staff members. Instead, pending orders are presented in pure FIFO (submission-
 * time) order — priority is completely ignored — so that whichever staff member
 * with the lightest current workload claims the next item at the top of the queue,
 * the distribution naturally stays balanced.
 */
public class LoadBalancingTriageStrategy implements TriageStrategy {

    private final OrderAccess orderAccess;

    public LoadBalancingTriageStrategy(OrderAccess orderAccess) {
        this.orderAccess = orderAccess;
    }

    /**
     * Returns the current in-progress workload per staff member.
     * Exposed so callers (e.g. a dashboard endpoint) can surface load information.
     */
    public Map<String, Long> getStaffWorkload() {
        return orderAccess.listAllOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS
                          && o.getClaimedBy() != null)
                .collect(Collectors.groupingBy(Order::getClaimedBy, Collectors.counting()));
    }

    /**
     * Sort pending orders by submission time (FIFO), ignoring priority entirely.
     * This ensures the oldest un-served order is always at the top of the queue
     * regardless of its priority level, enabling even load distribution.
     */
    @Override
    public List<Order> sort(List<Order> orders) {
        return orders.stream()
                .sorted(Comparator.comparing(Order::getSubmittedAt))
                .toList();
    }
}
