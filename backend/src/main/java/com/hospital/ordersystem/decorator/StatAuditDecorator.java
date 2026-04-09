package com.hospital.ordersystem.decorator;

import com.hospital.ordersystem.access.CommandLogAccess;
import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.model.CommandLogEntry;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderPriority;

public class StatAuditDecorator extends OrderHandlerDecorator {

    private final OrderAccess orderAccess;
    private final CommandLogAccess commandLogAccess;

    public StatAuditDecorator(OrderHandler next,
                              OrderAccess orderAccess,
                              CommandLogAccess commandLogAccess) {
        super(next);
        this.orderAccess = orderAccess;
        this.commandLogAccess = commandLogAccess;
    }

    @Override
    public Order handle(Order order) {
        Order processed = wrapped.handle(order);

        if (processed.getPriority() == OrderPriority.STAT) {
            long statCount = orderAccess.listAllOrders().stream()
                    .filter(o -> o.getPriority() == OrderPriority.STAT
                              && o.getType() == processed.getType())
                    .count() + 1;
            String detail = String.format("type=%s|stat_count=%d",
                    processed.getType(), statCount);
            commandLogAccess.append(
                    new CommandLogEntry("STAT_AUDIT", processed.getOrderId(), detail));
            System.out.printf(
                    "[STAT_AUDIT] STAT order %s | Type: %s | Running STAT count for type: %d%n",
                    processed.getOrderId(), processed.getType(), statCount);
        }
        return processed;
    }
}
