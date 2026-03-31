package com.hospital.ordersystem.command;

import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderStatus;
import com.hospital.ordersystem.observer.OrderEventPublisher;

public class ClaimOrderCommand implements OrderCommand {

    private final String orderId;
    private final String staffMember;
    private final OrderAccess orderAccess;
    private final OrderEventPublisher eventPublisher;

    public ClaimOrderCommand(String orderId, String staffMember,
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

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be claimed.");
        }
        if (order.getClaimedBy() != null) {
            throw new IllegalStateException("Order already claimed by: " + order.getClaimedBy());
        }

        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setClaimedBy(staffMember);
        orderAccess.saveOrder(order);
        eventPublisher.publish(order, "ORDER_CLAIMED");
    }

    @Override public String getCommandType() { return "CLAIM"; }
    @Override public String getOrderId()     { return orderId; }
    @Override public String getActor()       { return staffMember; }
}
