package com.hospital.ordersystem.command;

import com.hospital.ordersystem.access.CommandLogAccess;
import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.decorator.*;
import com.hospital.ordersystem.engine.TriagingEngine;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.observer.OrderEventPublisher;

import java.time.Clock;

public class StatAwareSubmitOrderCommand implements OrderCommand {

    private final Order order;
    private final OrderAccess orderAccess;
    private final TriagingEngine triagingEngine;
    private final OrderEventPublisher eventPublisher;
    private final Clock clock;
    private final CommandLogAccess commandLogAccess;

    public StatAwareSubmitOrderCommand(Order order,
                                       OrderAccess orderAccess,
                                       TriagingEngine triagingEngine,
                                       OrderEventPublisher eventPublisher,
                                       Clock clock,
                                       CommandLogAccess commandLogAccess) {
        this.order = order;
        this.orderAccess = orderAccess;
        this.triagingEngine = triagingEngine;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.commandLogAccess = commandLogAccess;
    }

    @Override
    public void execute() {
        OrderHandler chain = new StatAuditDecorator(
                new PriorityEscalationDecorator(
                        new AuditLoggingDecorator(
                                new ValidationDecorator(
                                        new BaseOrderHandler())),
                        orderAccess, clock),
                orderAccess, commandLogAccess);

        Order processed = chain.handle(order);
        orderAccess.saveOrder(processed);
        triagingEngine.requeue();
        eventPublisher.publish(processed, "ORDER_SUBMITTED");
    }

    @Override public String getCommandType() { return "SUBMIT"; }
    @Override public String getOrderId()     { return order.getOrderId(); }
    @Override public String getActor()       { return order.getClinician(); }
}
