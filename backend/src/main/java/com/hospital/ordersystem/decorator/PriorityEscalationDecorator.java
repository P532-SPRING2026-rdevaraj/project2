package com.hospital.ordersystem.decorator;

import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderPriority;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Escalation decorator: if the order being submitted is URGENT and a STAT
 * order of the same type was submitted within the last 5 minutes, the order
 * is automatically upgraded to STAT before being persisted.
 */
public class PriorityEscalationDecorator extends OrderHandlerDecorator {

    private final OrderAccess orderAccess;
    private final Clock clock;

    public PriorityEscalationDecorator(OrderHandler next,
                                       OrderAccess orderAccess,
                                       Clock clock) {
        super(next);
        this.orderAccess = orderAccess;
        this.clock = clock;
    }

    @Override
    public Order handle(Order order) {
        // Arrange: validate and log via inner chain first
        Order processed = wrapped.handle(order);

        // Act: escalate URGENT orders when a recent STAT of the same type exists
        if (processed.getPriority() == OrderPriority.URGENT) {
            Instant windowStart = clock.instant().minus(5, ChronoUnit.MINUTES);
            boolean recentStatExists = orderAccess.listAllOrders().stream()
                    .anyMatch(o -> o.getPriority() == OrderPriority.STAT
                               && o.getType() == processed.getType()
                               && o.getSubmittedAt()
                                   .atZone(clock.getZone())
                                   .toInstant()
                                   .isAfter(windowStart));
            if (recentStatExists) {
                processed.setPriority(OrderPriority.STAT);
                System.out.printf(
                        "[ESCALATION] Order %s upgraded URGENT→STAT " +
                        "(recent STAT %s within 5 min window)%n",
                        processed.getOrderId(), processed.getType());
            }
        }
        return processed;
    }
}
