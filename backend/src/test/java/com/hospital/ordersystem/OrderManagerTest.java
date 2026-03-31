package com.hospital.ordersystem;

import com.hospital.ordersystem.access.CommandLogAccess;
import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.engine.TriagingEngine;
import com.hospital.ordersystem.factory.OrderFactory;
import com.hospital.ordersystem.manager.OrderManager;
import com.hospital.ordersystem.model.*;
import com.hospital.ordersystem.observer.OrderEventPublisher;
import com.hospital.ordersystem.strategy.PriorityFirstTriageStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderManagerTest {

    private OrderManager orderManager;
    private OrderAccess orderAccess;

    @BeforeEach
    void setUp() {
        orderAccess = new OrderAccess();
        CommandLogAccess commandLogAccess = new CommandLogAccess();
        OrderFactory orderFactory = new OrderFactory();
        PriorityFirstTriageStrategy strategy = new PriorityFirstTriageStrategy();
        TriagingEngine triagingEngine = new TriagingEngine(orderAccess, strategy);
        OrderEventPublisher eventPublisher = new OrderEventPublisher();

        orderManager = new OrderManager(orderFactory, orderAccess, commandLogAccess,
                triagingEngine, eventPublisher);
    }

    @Test
    void submitOrder_createsOrderWithCorrectType() {
        Order order = orderManager.submitOrder(
                OrderType.LAB, "Alice", "Dr. Smith", "Blood panel", OrderPriority.ROUTINE);

        assertNotNull(order.getOrderId());
        assertEquals(OrderType.LAB, order.getType());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals("Alice", order.getPatientName());
    }

    @Test
    void submitOrder_recordsCommandLog() {
        orderManager.submitOrder(
                OrderType.MEDICATION, "Bob", "Dr. Jones", "Aspirin 100mg", OrderPriority.URGENT);

        List<CommandLogEntry> log = orderManager.getCommandLog();
        assertEquals(1, log.size());
        assertEquals("SUBMIT", log.get(0).getCommandType());
    }

    @Test
    void claimOrder_changesStatusToInProgress() {
        Order order = orderManager.submitOrder(
                OrderType.IMAGING, "Carol", "Dr. Lee", "Chest X-ray", OrderPriority.STAT);

        orderManager.claimOrder(order.getOrderId(), "Tech-1");

        Order updated = orderAccess.findOrderById(order.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.IN_PROGRESS, updated.getStatus());
        assertEquals("Tech-1", updated.getClaimedBy());
    }

    @Test
    void completeOrder_changesStatusToCompleted() {
        Order order = orderManager.submitOrder(
                OrderType.LAB, "Dave", "Dr. Kim", "Urine test", OrderPriority.ROUTINE);
        orderManager.claimOrder(order.getOrderId(), "Tech-2");
        orderManager.completeOrder(order.getOrderId(), "Tech-2");

        Order updated = orderAccess.findOrderById(order.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.COMPLETED, updated.getStatus());
    }

    @Test
    void cancelOrder_pendingOrder_succeeds() {
        Order order = orderManager.submitOrder(
                OrderType.MEDICATION, "Eve", "Dr. Patel", "Ibuprofen", OrderPriority.ROUTINE);
        orderManager.cancelOrder(order.getOrderId(), "Dr. Patel");

        Order updated = orderAccess.findOrderById(order.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, updated.getStatus());
    }

    @Test
    void cancelOrder_nonPendingOrder_throwsException() {
        Order order = orderManager.submitOrder(
                OrderType.LAB, "Frank", "Dr. Chen", "CBC", OrderPriority.URGENT);
        orderManager.claimOrder(order.getOrderId(), "Tech-3");

        assertThrows(IllegalStateException.class,
                () -> orderManager.cancelOrder(order.getOrderId(), "Dr. Chen"));
    }

    @Test
    void triageQueue_statOrdersBeforeRoutine() {
        orderManager.submitOrder(OrderType.LAB, "P1", "Dr. A", "Routine lab", OrderPriority.ROUTINE);
        orderManager.submitOrder(OrderType.LAB, "P2", "Dr. B", "Stat lab", OrderPriority.STAT);
        orderManager.submitOrder(OrderType.LAB, "P3", "Dr. C", "Urgent lab", OrderPriority.URGENT);

        List<Order> queue = orderManager.getQueue();
        // STAT first, then URGENT, then ROUTINE
        assertEquals(OrderPriority.STAT, queue.get(0).getPriority());
        assertEquals(OrderPriority.URGENT, queue.get(1).getPriority());
        assertEquals(OrderPriority.ROUTINE, queue.get(2).getPriority());
    }

    @Test
    void claimOrder_alreadyClaimed_throwsException() {
        Order order = orderManager.submitOrder(
                OrderType.IMAGING, "Grace", "Dr. Wu", "MRI scan", OrderPriority.URGENT);
        orderManager.claimOrder(order.getOrderId(), "Tech-1");

        assertThrows(IllegalStateException.class,
                () -> orderManager.claimOrder(order.getOrderId(), "Tech-2"));
    }
}
