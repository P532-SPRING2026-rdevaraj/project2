package com.hospital.ordersystem.factory;

import com.hospital.ordersystem.model.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Factory pattern — centralises creation of the correct Order subtype
 * (LabOrder, MedicationOrder, ImagingOrder) so callers are decoupled
 * from concrete classes.  Layer: Business Logic.
 *
 * Uses a registry map instead of a switch so adding a new OrderType only
 * requires adding one entry here — no other existing file changes needed.
 */
@Component
public class OrderFactory {

    @FunctionalInterface
    public interface OrderCreator {
        Order create(String id, String patient, String clinician,
                     String description, OrderPriority priority);
    }

    private final Map<OrderType, OrderCreator> registry = Map.of(
        OrderType.LAB,        LabOrder::new,
        OrderType.MEDICATION, MedicationOrder::new,
        OrderType.IMAGING,    ImagingOrder::new
    );

    public Order createOrder(OrderType type, String patientName, String clinician,
                             String description, OrderPriority priority) {
        OrderCreator creator = registry.get(type);
        if (creator == null) {
            throw new IllegalArgumentException("Unknown order type: " + type);
        }
        String orderId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return creator.create(orderId, patientName, clinician, description, priority);
    }
}
