package com.hospital.ordersystem.factory;

import com.hospital.ordersystem.model.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Factory pattern — centralises creation of the correct Order subtype
 * (LabOrder, MedicationOrder, ImagingOrder) so callers are decoupled
 * from concrete classes.  Layer: Business Logic.
 */
@Component
public class OrderFactory {

    public Order createOrder(OrderType type, String patientName, String clinician,
                             String description, OrderPriority priority) {
        String orderId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return switch (type) {
            case LAB        -> new LabOrder(orderId, patientName, clinician, description, priority);
            case MEDICATION -> new MedicationOrder(orderId, patientName, clinician, description, priority);
            case IMAGING    -> new ImagingOrder(orderId, patientName, clinician, description, priority);
        };
    }
}
