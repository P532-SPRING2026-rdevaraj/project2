package com.hospital.ordersystem;

import com.hospital.ordersystem.factory.OrderFactory;
import com.hospital.ordersystem.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderFactoryTest {

    private final OrderFactory factory = new OrderFactory();

    @Test
    void createLabOrder_returnsCorrectType() {
        Order o = factory.createOrder(OrderType.LAB, "P", "D", "desc", OrderPriority.ROUTINE);
        assertInstanceOf(LabOrder.class, o);
        assertEquals(OrderType.LAB, o.getType());
    }

    @Test
    void createMedicationOrder_returnsCorrectType() {
        Order o = factory.createOrder(OrderType.MEDICATION, "P", "D", "desc", OrderPriority.URGENT);
        assertInstanceOf(MedicationOrder.class, o);
        assertEquals(OrderType.MEDICATION, o.getType());
    }

    @Test
    void createImagingOrder_returnsCorrectType() {
        Order o = factory.createOrder(OrderType.IMAGING, "P", "D", "desc", OrderPriority.STAT);
        assertInstanceOf(ImagingOrder.class, o);
        assertEquals(OrderType.IMAGING, o.getType());
    }

    @Test
    void createdOrder_hasUniqueIds() {
        Order a = factory.createOrder(OrderType.LAB, "P", "D", "desc", OrderPriority.ROUTINE);
        Order b = factory.createOrder(OrderType.LAB, "P", "D", "desc", OrderPriority.ROUTINE);
        assertNotEquals(a.getOrderId(), b.getOrderId());
    }
}
