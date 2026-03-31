package com.hospital.ordersystem.decorator;

import com.hospital.ordersystem.model.Order;

/**
 * Abstract decorator — holds a reference to the wrapped handler and
 * delegates to it.  Concrete decorators extend this class.
 * Layer: Business Logic.
 */
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
