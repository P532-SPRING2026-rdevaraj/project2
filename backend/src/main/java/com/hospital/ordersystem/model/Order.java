package com.hospital.ordersystem.model;

import java.time.LocalDateTime;

public abstract class Order {

    private final String orderId;
    private final OrderType type;
    private final String patientName;
    private final String clinician;
    private final String description;
    private OrderPriority priority;
    private OrderStatus status;
    private final LocalDateTime submittedAt;
    private String claimedBy;

    protected Order(String orderId, OrderType type, String patientName,
                    String clinician, String description, OrderPriority priority) {
        this.orderId = orderId;
        this.type = type;
        this.patientName = patientName;
        this.clinician = clinician;
        this.description = description;
        this.priority = priority;
        this.status = OrderStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
    }

    public String getOrderId()       { return orderId; }
    public OrderType getType()       { return type; }
    public String getPatientName()   { return patientName; }
    public String getClinician()     { return clinician; }
    public String getDescription()   { return description; }
    public OrderPriority getPriority() { return priority; }
    public OrderStatus getStatus()   { return status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public String getClaimedBy()     { return claimedBy; }

    public void setStatus(OrderStatus status)     { this.status = status; }
    public void setClaimedBy(String claimedBy)   { this.claimedBy = claimedBy; }
    public void setPriority(OrderPriority priority) { this.priority = priority; }
}
