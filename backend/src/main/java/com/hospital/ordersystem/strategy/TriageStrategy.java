package com.hospital.ordersystem.strategy;

import com.hospital.ordersystem.model.Order;

import java.util.List;

public interface TriageStrategy {
    List<Order> sort(List<Order> orders);
}
