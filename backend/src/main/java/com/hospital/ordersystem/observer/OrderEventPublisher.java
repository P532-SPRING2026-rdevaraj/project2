package com.hospital.ordersystem.observer;

import com.hospital.ordersystem.model.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderEventPublisher {

    private final List<OrderObserver> observers = new ArrayList<>();

    public void subscribe(OrderObserver observer) {
        observers.add(observer);
    }

    public void publish(Order order, String event) {
        observers.forEach(o -> o.onOrderEvent(order, event));
    }
}
