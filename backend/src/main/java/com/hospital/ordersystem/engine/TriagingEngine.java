package com.hospital.ordersystem.engine;

import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderStatus;
import com.hospital.ordersystem.strategy.TriageStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Engine — encapsulates the triage algorithm. Uses the TriageStrategy so
 * the sorting policy can be changed at runtime without touching this class.
 * Layer: Business Logic (Engine).
 */
@Component
public class TriagingEngine {

    private final OrderAccess orderAccess;
    private TriageStrategy triageStrategy;

    public TriagingEngine(OrderAccess orderAccess, TriageStrategy triageStrategy) {
        this.orderAccess = orderAccess;
        this.triageStrategy = triageStrategy;
    }

    /** Returns all PENDING orders sorted by the active triage strategy. */
    public List<Order> getTriagedQueue() {
        List<Order> pending = orderAccess.listPendingOrders();
        return triageStrategy.sort(pending);
    }

    /** Returns all orders (all statuses) sorted by triage for the full queue view. */
    public List<Order> getAllOrders() {
        List<Order> all = orderAccess.listAllOrders();
        // Sort: PENDING first by priority, then IN_PROGRESS, COMPLETED, CANCELLED
        return all.stream()
                .sorted((a, b) -> {
                    int statusOrder = statusRank(a.getStatus()) - statusRank(b.getStatus());
                    if (statusOrder != 0) return statusOrder;
                    int priorityOrder = b.getPriority().getLevel() - a.getPriority().getLevel();
                    if (priorityOrder != 0) return priorityOrder;
                    return a.getSubmittedAt().compareTo(b.getSubmittedAt());
                })
                .toList();
    }

    /** Triggers a re-sort of the in-memory queue (no-op here; sorting is on-read). */
    public void requeue() {
        // The queue is sorted on every read via getTriagedQueue(), so no action needed.
    }

    /** Swap the triage strategy at runtime without restarting. */
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
