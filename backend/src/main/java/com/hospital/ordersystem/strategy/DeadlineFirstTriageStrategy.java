package com.hospital.ordersystem.strategy;

import com.hospital.ordersystem.model.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Deadline-first triage: each order is assigned a deadline based on its
 * priority. The queue is sorted by ascending time-to-deadline so the most
 * overdue work surfaces first.
 *
 * Deadlines (from submission time):
 *   STAT    → 30 minutes
 *   URGENT  → 2 hours
 *   ROUTINE → 8 hours
 */
@Component
public class DeadlineFirstTriageStrategy implements TriageStrategy {

    private static final Duration STAT_DEADLINE    = Duration.ofMinutes(30);
    private static final Duration URGENT_DEADLINE  = Duration.ofHours(2);
    private static final Duration ROUTINE_DEADLINE = Duration.ofHours(8);

    private final Clock clock;

    public DeadlineFirstTriageStrategy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<Order> sort(List<Order> orders) {
        Instant now = clock.instant();
        return orders.stream()
                .sorted(Comparator.comparingLong(o -> timeToDeadlineMillis(o, now)))
                .toList();
    }

    private long timeToDeadlineMillis(Order order, Instant now) {
        Duration allowance = switch (order.getPriority()) {
            case STAT    -> STAT_DEADLINE;
            case URGENT  -> URGENT_DEADLINE;
            case ROUTINE -> ROUTINE_DEADLINE;
        };
        Instant deadline = order.getSubmittedAt()
                .atZone(clock.getZone())
                .toInstant()
                .plus(allowance);
        return deadline.toEpochMilli() - now.toEpochMilli();
    }
}
