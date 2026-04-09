package com.hospital.ordersystem;

import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.factory.OrderFactory;
import com.hospital.ordersystem.model.*;
import com.hospital.ordersystem.strategy.DeadlineFirstTriageStrategy;
import com.hospital.ordersystem.strategy.LoadBalancingTriageStrategy;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers Change 1 — LoadBalancingTriageStrategy and DeadlineFirstTriageStrategy.
 */
class Week2TriageStrategyTest {

    private final OrderFactory factory = new OrderFactory();

    // ── LoadBalancingTriageStrategy ───────────────────────────────────────────

    @Test
    void loadBalancing_sortsBySubmissionTimeFIFO() {
        // Arrange
        OrderAccess orderAccess = mock(OrderAccess.class);
        when(orderAccess.listAllOrders()).thenReturn(List.of());
        LoadBalancingTriageStrategy strategy = new LoadBalancingTriageStrategy(orderAccess);
        Order stat    = factory.createOrder(OrderType.LAB,      "P1", "Dr. A", "d", OrderPriority.STAT);
        Order routine = factory.createOrder(OrderType.IMAGING,  "P2", "Dr. B", "d", OrderPriority.ROUTINE);
        Order urgent  = factory.createOrder(OrderType.MEDICATION,"P3", "Dr. C", "d", OrderPriority.URGENT);

        // Act
        List<Order> sorted = strategy.sort(List.of(stat, routine, urgent));

        // Assert: sorted by submission time (FIFO), ignoring priority
        assertEquals(stat.getOrderId(),    sorted.get(0).getOrderId());
        assertEquals(routine.getOrderId(), sorted.get(1).getOrderId());
        assertEquals(urgent.getOrderId(),  sorted.get(2).getOrderId());
    }

    @Test
    void loadBalancing_ignoresPriority() {
        // Arrange
        OrderAccess orderAccess = mock(OrderAccess.class);
        when(orderAccess.listAllOrders()).thenReturn(List.of());
        LoadBalancingTriageStrategy strategy = new LoadBalancingTriageStrategy(orderAccess);
        Order routine = factory.createOrder(OrderType.LAB, "P1", "Dr. A", "d", OrderPriority.ROUTINE);
        Order stat    = factory.createOrder(OrderType.LAB, "P2", "Dr. B", "d", OrderPriority.STAT);

        // Act — ROUTINE was created first, so it should lead despite lower priority
        List<Order> sorted = strategy.sort(List.of(routine, stat));

        // Assert
        assertEquals(routine.getOrderId(), sorted.get(0).getOrderId());
        assertEquals(stat.getOrderId(),    sorted.get(1).getOrderId());
    }

    // ── DeadlineFirstTriageStrategy ───────────────────────────────────────────

    @Test
    void deadlineFirst_statBeforeUrgentBeforeRoutine() {
        // Arrange — fix clock so deadline arithmetic is deterministic
        Clock fixed = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        DeadlineFirstTriageStrategy strategy = new DeadlineFirstTriageStrategy(fixed);

        Order routine = factory.createOrder(OrderType.LAB,      "P1", "Dr. A", "d", OrderPriority.ROUTINE);
        Order urgent  = factory.createOrder(OrderType.MEDICATION,"P2", "Dr. B", "d", OrderPriority.URGENT);
        Order stat    = factory.createOrder(OrderType.IMAGING,   "P3", "Dr. C", "d", OrderPriority.STAT);

        // Act
        List<Order> sorted = strategy.sort(List.of(routine, urgent, stat));

        // Assert: STAT deadline (30 min) < URGENT (2 h) < ROUTINE (8 h)
        assertEquals(OrderPriority.STAT,    sorted.get(0).getPriority());
        assertEquals(OrderPriority.URGENT,  sorted.get(1).getPriority());
        assertEquals(OrderPriority.ROUTINE, sorted.get(2).getPriority());
    }

    @Test
    void deadlineFirst_samePrioritySortedBySubmissionTime() {
        // Arrange
        Clock fixed = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        DeadlineFirstTriageStrategy strategy = new DeadlineFirstTriageStrategy(fixed);

        Order first  = factory.createOrder(OrderType.LAB, "P1", "Dr. A", "d", OrderPriority.URGENT);
        Order second = factory.createOrder(OrderType.LAB, "P2", "Dr. B", "d", OrderPriority.URGENT);

        // Act
        List<Order> sorted = strategy.sort(List.of(first, second));

        // Assert: earlier submission → earlier deadline among same-priority orders
        assertEquals(first.getOrderId(), sorted.get(0).getOrderId());
    }
}
