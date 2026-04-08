package com.hospital.ordersystem.command;

import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderPriority;
import com.hospital.ordersystem.model.OrderStatus;

/**
 * Decorator that gives any {@link OrderCommand} single-level undo capability
 * without touching the wrapped command.
 *
 * Before {@code execute()} it takes a snapshot of the mutable Order fields
 * (status, claimedBy, priority).  If the order did not yet exist (SUBMIT),
 * the snapshot is absent and undo deletes the order.  Otherwise undo restores
 * the captured mutable fields — reversing CANCEL, CLAIM, or COMPLETE in one
 * place rather than in each command class.
 */
public class UndoableCommandDecorator implements OrderCommand {

    private final OrderCommand wrapped;
    private final OrderAccess  orderAccess;

    // Snapshot captured just before execute()
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
        // Arrange: capture current state before the command mutates it
        orderAccess.findOrderById(wrapped.getOrderId()).ifPresent(o -> {
            orderExistedBefore = true;
            snapshotStatus     = o.getStatus();
            snapshotClaimedBy  = o.getClaimedBy();
            snapshotPriority   = o.getPriority();
        });

        // Act: run the wrapped command
        wrapped.execute();
    }

    @Override
    public void undo() {
        if (!orderExistedBefore) {
            // SUBMIT undo: order was not in the store before → delete it
            orderAccess.deleteOrder(wrapped.getOrderId());
        } else {
            // CANCEL / CLAIM / COMPLETE undo: restore captured snapshot
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
