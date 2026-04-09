package com.hospital.ordersystem.decorator;

import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.model.OrderPriority;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
        Order processed = wrapped.handle(order);

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
