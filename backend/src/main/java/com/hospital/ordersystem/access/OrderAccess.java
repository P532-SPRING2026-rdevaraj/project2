package com.hospital.ordersystem.access;

import com.hospital.ordersystem.model.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource Access layer — encapsulates all access to the in-memory order store.
 * Exposes atomic business verbs so the Manager never touches raw collections.
 * Layer: Resource Access.
 */
@Component
public class OrderAccess {

    private final Map<String, Order> store = new ConcurrentHashMap<>();

    public void saveOrder(Order order) {
        store.put(order.getOrderId(), order);
    }

    public Optional<Order> findOrderById(String orderId) {
        return Optional.ofNullable(store.get(orderId));
    }

    public List<Order> listAllOrders() {
        return new ArrayList<>(store.values());
    }

    public List<Order> listPendingOrders() {
        return store.values().stream()
                .filter(o -> o.getStatus().name().equals("PENDING"))
                .toList();
    }
}
