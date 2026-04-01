package com.hospital.ordersystem.command;

import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.decorator.AuditLoggingDecorator;
import com.hospital.ordersystem.decorator.BaseOrderHandler;
import com.hospital.ordersystem.decorator.OrderHandler;
import com.hospital.ordersystem.decorator.ValidationDecorator;
import com.hospital.ordersystem.engine.TriagingEngine;
import com.hospital.ordersystem.model.Order;
import com.hospital.ordersystem.observer.OrderEventPublisher;

public class SubmitOrderCommand implements OrderCommand {

    private final Order order;
    private final OrderAccess orderAccess;
    private final TriagingEngine triagingEngine;
    private final OrderEventPublisher eventPublisher;

    public SubmitOrderCommand(Order order, OrderAccess orderAccess,
                              TriagingEngine triagingEngine,
                              OrderEventPublisher eventPublisher) {
        this.order = order;
        this.orderAccess = orderAccess;
        this.triagingEngine = triagingEngine;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute() {
        OrderHandler handler = new AuditLoggingDecorator(
                                   new ValidationDecorator(
                                       new BaseOrderHandler()));
        Order processed = handler.handle(order);
        orderAccess.saveOrder(processed);
        triagingEngine.requeue();
        eventPublisher.publish(processed, "ORDER_SUBMITTED");
    }

    @Override public String getCommandType() { return "SUBMIT"; }
    @Override public String getOrderId()     { return order.getOrderId(); }
    @Override public String getActor()       { return order.getClinician(); }
}
