package com.hospital.ordersystem.manager;

import com.hospital.ordersystem.access.CommandLogAccess;
import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.command.*;
import com.hospital.ordersystem.engine.TriagingEngine;
import com.hospital.ordersystem.factory.OrderFactory;
import com.hospital.ordersystem.model.*;
import com.hospital.ordersystem.observer.OrderEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderManager {

    private final OrderFactory orderFactory;
    private final OrderAccess orderAccess;
    private final CommandLogAccess commandLogAccess;
    private final TriagingEngine triagingEngine;
    private final OrderEventPublisher eventPublisher;

    public OrderManager(OrderFactory orderFactory,
                        OrderAccess orderAccess,
                        CommandLogAccess commandLogAccess,
                        TriagingEngine triagingEngine,
                        OrderEventPublisher eventPublisher) {
        this.orderFactory = orderFactory;
        this.orderAccess = orderAccess;
        this.commandLogAccess = commandLogAccess;
        this.triagingEngine = triagingEngine;
        this.eventPublisher = eventPublisher;
    }

    public Order submitOrder(OrderType type, String patientName, String clinician,
                             String description, OrderPriority priority) {
        Order order = orderFactory.createOrder(type, patientName, clinician, description, priority);
        dispatch(new SubmitOrderCommand(order, orderAccess, triagingEngine, eventPublisher));
        return order;
    }

    public void claimOrder(String orderId, String staffMember) {
        dispatch(new ClaimOrderCommand(orderId, staffMember, orderAccess, eventPublisher));
    }

    public void completeOrder(String orderId, String staffMember) {
        dispatch(new CompleteOrderCommand(orderId, staffMember, orderAccess, eventPublisher));
    }

    public void cancelOrder(String orderId, String clinician) {
        dispatch(new CancelOrderCommand(orderId, clinician, orderAccess, eventPublisher));
    }

    public List<Order> getQueue() {
        return triagingEngine.getAllOrders();
    }

    public List<CommandLogEntry> getCommandLog() {
        return commandLogAccess.getAll();
    }

    private void dispatch(OrderCommand cmd) {
        cmd.execute();
        commandLogAccess.append(new CommandLogEntry(cmd.getCommandType(), cmd.getOrderId(), cmd.getActor()));
    }
}
