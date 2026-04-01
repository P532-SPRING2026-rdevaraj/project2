package com.hospital.ordersystem.decorator;

import com.hospital.ordersystem.model.Order;

public abstract class OrderHandlerDecorator implements OrderHandler {

    protected final OrderHandler wrapped;

    protected OrderHandlerDecorator(OrderHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Order handle(Order order) {
        return wrapped.handle(order);
    }
}
