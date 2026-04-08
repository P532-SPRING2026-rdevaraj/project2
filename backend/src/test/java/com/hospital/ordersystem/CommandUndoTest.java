package com.hospital.ordersystem;

import com.hospital.ordersystem.access.CommandLogAccess;
import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.engine.TriagingEngine;
import com.hospital.ordersystem.factory.OrderFactory;
import com.hospital.ordersystem.manager.OrderManager;
import com.hospital.ordersystem.model.*;
import com.hospital.ordersystem.observer.OrderEventPublisher;
import com.hospital.ordersystem.strategy.PriorityFirstTriageStrategy;
import com.hospital.ordersystem.strategy.TriageStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers Change 3 — undo() on each command type and replay from the audit log.
 */
class CommandUndoTest {

    private OrderManager orderManager;
    private OrderAccess orderAccess;
    private OrderFactory factory;

    @BeforeEach
    void setUp() {
        orderAccess = new OrderAccess();
        CommandLogAccess commandLogAccess = new CommandLogAccess();
        factory = new OrderFactory();
        PriorityFirstTriageStrategy strategy = new PriorityFirstTriageStrategy();
        TriagingEngine triagingEngine = new TriagingEngine(orderAccess, strategy);
        OrderEventPublisher eventPublisher = new OrderEventPublisher();
        Map<String, TriageStrategy> strategies =
                Map.of("priorityFirstTriageStrategy", strategy);

        orderManager = new OrderManager(factory, orderAccess, commandLogAccess,
                triagingEngine, eventPublisher, Clock.systemDefaultZone(), strategies);
    }

    // ── CancelCommand undo ────────────────────────────────────────────────────

    @Test
    void undo_cancelCommand_restoresOrderToPending() {
        // Arrange
        Order order = orderManager.submitOrder(
                OrderType.LAB, "Alice", "Dr. Smith", "CBC", OrderPriority.ROUTINE);
        orderManager.cancelOrder(order.getOrderId(), "Dr. Smith");

        // Act
        orderManager.undoLastCommand();

        // Assert: order back to PENDING
        Order restored = orderAccess.findOrderById(order.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.PENDING, restored.getStatus());
    }

    // ── ClaimCommand undo ─────────────────────────────────────────────────────

    @Test
    void undo_claimCommand_restoresOrderToPendingAndClearsClaim() {
        // Arrange
        Order order = orderManager.submitOrder(
                OrderType.MEDICATION, "Bob", "Dr. Jones", "Aspirin", OrderPriority.URGENT);
        orderManager.claimOrder(order.getOrderId(), "Tech-1");

        // Act
        orderManager.undoLastCommand();

        // Assert
        Order restored = orderAccess.findOrderById(order.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.PENDING, restored.getStatus());
        assertNull(restored.getClaimedBy());
    }

    // ── CompleteCommand undo ──────────────────────────────────────────────────

    @Test
    void undo_completeCommand_restoresOrderToInProgress() {
        // Arrange
        Order order = orderManager.submitOrder(
                OrderType.IMAGING, "Carol", "Dr. Lee", "X-Ray", OrderPriority.ROUTINE);
        orderManager.claimOrder(order.getOrderId(), "Tech-2");
        orderManager.completeOrder(order.getOrderId(), "Tech-2");

        // Act
        orderManager.undoLastCommand();

        // Assert
        Order restored = orderAccess.findOrderById(order.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.IN_PROGRESS, restored.getStatus());
    }

    // ── SubmitCommand undo ────────────────────────────────────────────────────

    @Test
    void undo_submitCommand_removesOrderFromStore() {
        // Arrange
        Order order = orderManager.submitOrder(
                OrderType.LAB, "Dave", "Dr. Kim", "MRI", OrderPriority.STAT);

        // Act
        orderManager.undoLastCommand();

        // Assert: order deleted from store
        assertTrue(orderAccess.findOrderById(order.getOrderId()).isEmpty());
    }

    // ── No command to undo ────────────────────────────────────────────────────

    @Test
    void undo_noCommand_throwsException() {
        // Arrange — nothing dispatched yet

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> orderManager.undoLastCommand());
    }

    // ── Replay ───────────────────────────────────────────────────────────────

    @Test
    void replay_cancelledOrder_resubmitsItAsPending() {
        // Arrange: submit then cancel an order
        Order order = orderManager.submitOrder(
                OrderType.MEDICATION, "Eve", "Dr. Patel", "Ibuprofen", OrderPriority.ROUTINE);
        orderManager.cancelOrder(order.getOrderId(), "Dr. Patel");

        // Find the SUBMIT entry index in the log
        int submitIndex = 0; // first entry is SUBMIT

        // Act: replay the original SUBMIT
        orderManager.replayCommand(submitIndex);

        // Assert: order exists and is back to PENDING
        Order replayed = orderAccess.findOrderById(order.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.PENDING, replayed.getStatus());
    }
}
