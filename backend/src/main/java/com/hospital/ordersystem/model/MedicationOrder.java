package com.hospital.ordersystem.model;

public class MedicationOrder extends Order {

    public MedicationOrder(String orderId, String patientName, String clinician,
                           String description, OrderPriority priority) {
        super(orderId, OrderType.MEDICATION, patientName, clinician, description, priority);
    }
}
