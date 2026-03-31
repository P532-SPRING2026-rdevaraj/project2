package com.hospital.ordersystem.command;

import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderStatus;
import com.hospital.ordersystem.observer.OrderEventPublisher;

public class CompleteOrderCommand implements OrderCommand {

    private final String orderId;
    private final String staffMember;
    private final OrderAccess orderAccess;
    private final OrderEventPublisher eventPublisher;

    public CompleteOrderCommand(String orderId, String staffMember,
                                OrderAccess orderAccess,
                                OrderEventPublisher eventPublisher) {
        this.orderId = orderId;
        this.staffMember = staffMember;
        this.orderAccess = orderAccess;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute() {
        Order order = orderAccess.findOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only IN_PROGRESS orders can be completed.");
        }
        if (!staffMember.equals(order.getClaimedBy())) {
            throw new IllegalStateException("Order is claimed by a different staff member.");
        }

        order.setStatus(OrderStatus.COMPLETED);
        orderAccess.saveOrder(order);
        eventPublisher.publish(order, "ORDER_COMPLETED");
    }

    @Override public String getCommandType() { return "COMPLETE"; }
    @Override public String getOrderId()     { return orderId; }
    @Override public String getActor()       { return staffMember; }
}
