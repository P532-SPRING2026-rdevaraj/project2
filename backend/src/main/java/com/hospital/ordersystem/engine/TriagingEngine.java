package com.hospital.ordersystem.engine;

import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderStatus;
import com.hospital.ordersystem.strategy.TriageStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TriagingEngine {

    private final OrderAccess orderAccess;
    private TriageStrategy triageStrategy;

    public TriagingEngine(OrderAccess orderAccess, TriageStrategy triageStrategy) {
        this.orderAccess = orderAccess;
        this.triageStrategy = triageStrategy;
    }

    public List<Order> getTriagedQueue() {
        return triageStrategy.sort(orderAccess.listPendingOrders());
    }

    public List<Order> getAllOrders() {
        List<Order> all = orderAccess.listAllOrders();

        List<Order> pending = triageStrategy.sort(
                all.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).toList());

        List<Order> rest = all.stream()
                .filter(o -> o.getStatus() != OrderStatus.PENDING)
                .sorted((a, b) -> {
                    int statusOrder = statusRank(a.getStatus()) - statusRank(b.getStatus());
                    if (statusOrder != 0) return statusOrder;
                    return a.getSubmittedAt().compareTo(b.getSubmittedAt());
                })
                .toList();

        return java.util.stream.Stream.concat(pending.stream(), rest.stream()).toList();
    }

    public void requeue() {}

    public void setTriageStrategy(TriageStrategy triageStrategy) {
        this.triageStrategy = triageStrategy;
    }

    private int statusRank(OrderStatus status) {
        return switch (status) {
            case PENDING     -> 0;
            case IN_PROGRESS -> 1;
            case COMPLETED   -> 2;
            case CANCELLED   -> 3;
        };
    }
}
