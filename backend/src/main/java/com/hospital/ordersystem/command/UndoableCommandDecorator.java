package com.hospital.ordersystem.command;

import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderPriority;
import com.hospital.ordersystem.model.OrderStatus;

public class UndoableCommandDecorator implements OrderCommand {

    private final OrderCommand wrapped;
    private final OrderAccess  orderAccess;

    private boolean       orderExistedBefore = false;
    private OrderStatus   snapshotStatus;
    private String        snapshotClaimedBy;
    private OrderPriority snapshotPriority;

    public UndoableCommandDecorator(OrderCommand wrapped, OrderAccess orderAccess) {
        this.wrapped     = wrapped;
        this.orderAccess = orderAccess;
    }

    @Override
    public void execute() {
        orderAccess.findOrderById(wrapped.getOrderId()).ifPresent(o -> {
            orderExistedBefore = true;
            snapshotStatus     = o.getStatus();
            snapshotClaimedBy  = o.getClaimedBy();
            snapshotPriority   = o.getPriority();
        });

        wrapped.execute();
    }

    @Override
    public void undo() {
        if (!orderExistedBefore) {
            orderAccess.deleteOrder(wrapped.getOrderId());
        } else {
            Order order = orderAccess.findOrderById(wrapped.getOrderId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Order not found for undo: " + wrapped.getOrderId()));
            order.setStatus(snapshotStatus);
            order.setClaimedBy(snapshotClaimedBy);
            order.setPriority(snapshotPriority);
            orderAccess.saveOrder(order);
        }
    }

    @Override public String getCommandType() { return wrapped.getCommandType(); }
    @Override public String getOrderId()     { return wrapped.getOrderId(); }
    @Override public String getActor()       { return wrapped.getActor(); }
}
