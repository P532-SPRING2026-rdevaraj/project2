package com.hospital.ordersystem;

import com.hospital.ordersystem.access.CommandLogAccess;
import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.decorator.*;
import com.hospital.ordersystem.factory.OrderFactory;
import com.hospital.ordersystem.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers Change 2b — PriorityEscalationDecorator and StatAuditDecorator.
 * Uses a fixed Clock so all temporal checks are deterministic.
 */
class ProcessingDecoratorTest {

    private OrderAccess orderAccess;
    private CommandLogAccess commandLogAccess;
    private Clock fixedClock;
    private OrderFactory factory;

    @BeforeEach
    void setUp() {
        orderAccess      = new OrderAccess();
        commandLogAccess = new CommandLogAccess();
        fixedClock       = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        factory          = new OrderFactory();
    }

    private OrderHandler buildChain() {
        return new StatAuditDecorator(
                new PriorityEscalationDecorator(
                        new AuditLoggingDecorator(
                                new ValidationDecorator(
                                        new BaseOrderHandler())),
                        orderAccess, fixedClock),
                orderAccess, commandLogAccess);
    }

    @Test
    void escalation_urgentWithinWindow_upgradedToStat() {
        // Arrange: persist a STAT LAB order so the window check fires
        Order statOrder = factory.createOrder(
                OrderType.LAB, "Alice", "Dr. Smith", "Stat lab", OrderPriority.STAT);
        orderAccess.saveOrder(statOrder);

        Order urgentOrder = factory.createOrder(
                OrderType.LAB, "Bob", "Dr. Jones", "Urgent lab", OrderPriority.URGENT);

        // Act
        Order result = buildChain().handle(urgentOrder);

        // Assert: URGENT was escalated to STAT because a recent STAT LAB exists
        assertEquals(OrderPriority.STAT, result.getPriority());
    }

    @Test
    void escalation_urgentOutsideWindow_notUpgraded() {
        // Arrange: no STAT orders in store → escalation condition is false
        Order urgentOrder = factory.createOrder(
                OrderType.LAB, "Carol", "Dr. Lee", "Urgent lab", OrderPriority.URGENT);

        // Act
        Order result = buildChain().handle(urgentOrder);

        // Assert: priority unchanged
        assertEquals(OrderPriority.URGENT, result.getPriority());
    }

    @Test
    void escalation_differentType_notUpgraded() {
        // Arrange: STAT IMAGING exists, but submitting URGENT LAB — different type
        Order statImaging = factory.createOrder(
                OrderType.IMAGING, "Dave", "Dr. Kim", "Stat scan", OrderPriority.STAT);
        orderAccess.saveOrder(statImaging);

        Order urgentLab = factory.createOrder(
                OrderType.LAB, "Eve", "Dr. Patel", "Urgent lab", OrderPriority.URGENT);

        // Act
        Order result = buildChain().handle(urgentLab);

        // Assert: not escalated — different order type
        assertEquals(OrderPriority.URGENT, result.getPriority());
    }

    @Test
    void statAudit_statOrder_appendsAuditEntry() {
        // Arrange
        Order statOrder = factory.createOrder(
                OrderType.LAB, "Frank", "Dr. Chen", "Stat lab", OrderPriority.STAT);

        // Act
        buildChain().handle(statOrder);

        // Assert: a STAT_AUDIT entry was appended to the command log
        List<CommandLogEntry> log = commandLogAccess.getAll();
        assertTrue(log.stream().anyMatch(e -> e.getCommandType().equals("STAT_AUDIT")));
    }

    @Test
    void statAudit_routineOrder_noAuditEntry() {
        // Arrange
        Order routineOrder = factory.createOrder(
                OrderType.MEDICATION, "Grace", "Dr. Wu", "Routine med", OrderPriority.ROUTINE);

        // Act
        buildChain().handle(routineOrder);

        // Assert: no STAT_AUDIT entry for non-STAT order
        assertTrue(commandLogAccess.getAll().isEmpty());
    }
}
