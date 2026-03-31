package com.hospital.ordersystem.model;

public class LabOrder extends Order {

    public LabOrder(String orderId, String patientName, String clinician,
                    String description, OrderPriority priority) {
        super(orderId, OrderType.LAB, patientName, clinician, description, priority);
    }
}
