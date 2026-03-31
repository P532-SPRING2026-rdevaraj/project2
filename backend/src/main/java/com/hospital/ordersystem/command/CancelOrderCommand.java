package com.hospital.ordersystem.command;

import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderStatus;
import com.hospital.ordersystem.observer.OrderEventPublisher;

public class CancelOrderCommand implements OrderCommand {

    private final String orderId;
    private final String clinician;
    private final OrderAccess orderAccess;
    private final OrderEventPublisher eventPublisher;

    public CancelOrderCommand(String orderId, String clinician,
                              OrderAccess orderAccess,
                              OrderEventPublisher eventPublisher) {
        this.orderId = orderId;
        this.clinician = clinician;
        this.orderAccess = orderAccess;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute() {
        Order order = orderAccess.findOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                "Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderAccess.saveOrder(order);
        eventPublisher.publish(order, "ORDER_CANCELLED");
    }

    @Override public String getCommandType() { return "CANCEL"; }
    @Override public String getOrderId()     { return orderId; }
    @Override public String getActor()       { return clinician; }
}
