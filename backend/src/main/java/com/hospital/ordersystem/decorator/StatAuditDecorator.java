package com.hospital.ordersystem.decorator;

import com.hospital.ordersystem.access.CommandLogAccess;
import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.model.CommandLogEntry;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderPriority;

/**
 * STAT audit decorator: when the processed order is STAT, records extra
 * detail in the command log — total STAT orders of that type and the
 * order's escalation state — and prints a structured console summary.
 */
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
        // Act: run full inner chain (validate → log → escalate)
        Order processed = wrapped.handle(order);

        // Assert/Record: if STAT, append extra audit detail
        if (processed.getPriority() == OrderPriority.STAT) {
            long statCount = orderAccess.listAllOrders().stream()
                    .filter(o -> o.getPriority() == OrderPriority.STAT
                              && o.getType() == processed.getType())
                    .count() + 1; // +1 for the order being submitted now
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
