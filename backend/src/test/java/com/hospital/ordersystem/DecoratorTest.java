package com.hospital.ordersystem;

import com.hospital.ordersystem.decorator.*;
import com.hospital.ordersystem.factory.OrderFactory;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderPriority;
import com.hospital.ordersystem.model.OrderType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecoratorTest {

    private final OrderFactory factory = new OrderFactory();

    @Test
    void validationDecorator_rejectsBlankPatientName() {
        Order order = factory.createOrder(OrderType.LAB, "", "Dr. A", "desc", OrderPriority.ROUTINE);
        OrderHandler handler = new ValidationDecorator(new BaseOrderHandler());
        assertThrows(IllegalArgumentException.class, () -> handler.handle(order));
    }

    @Test
    void validationDecorator_rejectsBlankClinician() {
        Order order = factory.createOrder(OrderType.LAB, "Patient", "", "desc", OrderPriority.ROUTINE);
        OrderHandler handler = new ValidationDecorator(new BaseOrderHandler());
        assertThrows(IllegalArgumentException.class, () -> handler.handle(order));
    }

    @Test
    void validationDecorator_passesValidOrder() {
        Order order = factory.createOrder(OrderType.LAB, "Alice", "Dr. Smith", "CBC", OrderPriority.STAT);
        OrderHandler handler = new ValidationDecorator(new BaseOrderHandler());
        assertDoesNotThrow(() -> handler.handle(order));
    }

    @Test
    void auditLoggingDecorator_returnsOrder() {
        Order order = factory.createOrder(OrderType.IMAGING, "Bob", "Dr. Lee", "X-ray", OrderPriority.URGENT);
        OrderHandler handler = new AuditLoggingDecorator(new ValidationDecorator(new BaseOrderHandler()));
        Order result = handler.handle(order);
        assertNotNull(result);
        assertEquals(order.getOrderId(), result.getOrderId());
    }
}
