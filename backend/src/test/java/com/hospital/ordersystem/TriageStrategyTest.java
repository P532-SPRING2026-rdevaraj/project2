package com.hospital.ordersystem;

import com.hospital.ordersystem.factory.OrderFactory;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderPriority;
import com.hospital.ordersystem.model.OrderType;
import com.hospital.ordersystem.strategy.PriorityFirstTriageStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TriageStrategyTest {

    private final OrderFactory factory = new OrderFactory();
    private final PriorityFirstTriageStrategy strategy = new PriorityFirstTriageStrategy();

    @Test
    void sort_statFirst() {
        Order routine = factory.createOrder(OrderType.LAB, "P1", "D1", "desc", OrderPriority.ROUTINE);
        Order stat    = factory.createOrder(OrderType.LAB, "P2", "D2", "desc", OrderPriority.STAT);
        Order urgent  = factory.createOrder(OrderType.LAB, "P3", "D3", "desc", OrderPriority.URGENT);

        List<Order> sorted = strategy.sort(List.of(routine, urgent, stat));

        assertEquals(OrderPriority.STAT,    sorted.get(0).getPriority());
        assertEquals(OrderPriority.URGENT,  sorted.get(1).getPriority());
        assertEquals(OrderPriority.ROUTINE, sorted.get(2).getPriority());
    }

    @Test
    void sort_tiesBreakBySubmissionTime() throws InterruptedException {
        Order first  = factory.createOrder(OrderType.LAB, "P1", "D1", "desc", OrderPriority.URGENT);
        Thread.sleep(10);
        Order second = factory.createOrder(OrderType.LAB, "P2", "D2", "desc", OrderPriority.URGENT);

        List<Order> sorted = strategy.sort(List.of(second, first));

        assertEquals(first.getOrderId(), sorted.get(0).getOrderId());
    }
}
