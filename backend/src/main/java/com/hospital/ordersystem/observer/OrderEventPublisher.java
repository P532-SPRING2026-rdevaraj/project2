package com.hospital.ordersystem.observer;

import com.hospital.ordersystem.model.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Subject in the Observer pattern. Maintains the list of observers and
 * notifies them whenever an order event occurs.  Layer: Business Logic.
 */
@Component
public class OrderEventPublisher {

    private final List<OrderObserver> observers = new ArrayList<>();

    public void subscribe(OrderObserver observer) {
        observers.add(observer);
    }

    public void publish(Order order, String event) {
        for (OrderObserver observer : observers) {
            observer.onOrderEvent(order, event);
        }
    }
}
