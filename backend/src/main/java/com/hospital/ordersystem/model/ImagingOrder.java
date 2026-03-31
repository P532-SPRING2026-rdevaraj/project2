package com.hospital.ordersystem.model;

public class ImagingOrder extends Order {

    public ImagingOrder(String orderId, String patientName, String clinician,
                        String description, OrderPriority priority) {
        super(orderId, OrderType.IMAGING, patientName, clinician, description, priority);
    }
}
