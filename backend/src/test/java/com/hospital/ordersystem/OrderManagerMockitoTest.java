package com.hospital.ordersystem;

import com.hospital.ordersystem.access.CommandLogAccess;
import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.engine.TriagingEngine;
import com.hospital.ordersystem.factory.OrderFactory;
import com.hospital.ordersystem.manager.OrderManager;
import com.hospital.ordersystem.model.*;
import com.hospital.ordersystem.observer.OrderEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Mockito-based tests that isolate OrderManager from its dependencies.
 * Uses mocks to verify interaction contracts between layers.
 */
@ExtendWith(MockitoExtension.class)
class OrderManagerMockitoTest {

    @Mock private OrderAccess orderAccess;
    @Mock private CommandLogAccess commandLogAccess;
    @Mock private TriagingEngine triagingEngine;
    @Mock private OrderEventPublisher eventPublisher;

    private OrderManager orderManager;
    private OrderFactory orderFactory;

    @BeforeEach
    void setUp() {
        orderFactory = new OrderFactory();
        // Stub listAllOrders for STAT/URGENT decorator paths (lenient: not all tests trigger it)
        lenient().when(orderAccess.listAllOrders()).thenReturn(List.of());
        orderManager = new OrderManager(orderFactory, orderAccess, commandLogAccess,
                triagingEngine, eventPublisher, Clock.systemDefaultZone(), Map.of());
    }

    @Test
    void submitOrder_savesOrderToAccess() {
        orderManager.submitOrder(OrderType.LAB, "Alice", "Dr. Smith", "CBC", OrderPriority.STAT);

        verify(orderAccess, times(1)).saveOrder(any(Order.class));
    }

    @Test
    void submitOrder_publishesEvent() {
        orderManager.submitOrder(OrderType.MEDICATION, "Bob", "Dr. Lee", "Aspirin", OrderPriority.URGENT);

        verify(eventPublisher, times(1)).publish(any(Order.class), eq("ORDER_SUBMITTED"));
    }

    @Test
    void submitOrder_logsCommandEntry() {
        orderManager.submitOrder(OrderType.IMAGING, "Carol", "Dr. Kim", "MRI", OrderPriority.ROUTINE);

        verify(commandLogAccess, times(1)).append(any(CommandLogEntry.class));
    }

    @Test
    void claimOrder_publishesClaimEvent() {
        Order order = orderFactory.createOrder(
                OrderType.LAB, "Dave", "Dr. Wu", "Blood test", OrderPriority.URGENT);
        when(orderAccess.findOrderById(order.getOrderId()))
                .thenReturn(java.util.Optional.of(order));

        orderManager.claimOrder(order.getOrderId(), "Tech-1");

        verify(eventPublisher, times(1)).publish(any(Order.class), eq("ORDER_CLAIMED"));
    }

    @Test
    void cancelOrder_nonExistentOrder_throwsException() {
        when(orderAccess.findOrderById(anyString())).thenReturn(java.util.Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> orderManager.cancelOrder("FAKE-ID", "Dr. X"));

        verify(eventPublisher, never()).publish(any(), anyString());
    }

    @Test
    void getCommandLog_delegatesToCommandLogAccess() {
        when(commandLogAccess.getAll()).thenReturn(List.of());

        orderManager.getCommandLog();

        verify(commandLogAccess, times(1)).getAll();
    }

    @Test
    void getQueue_delegatesToTriagingEngine() {
        when(triagingEngine.getAllOrders()).thenReturn(List.of());

        orderManager.getQueue();

        verify(triagingEngine, times(1)).getAllOrders();
    }
}
