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

/**
 * Manager — orchestrates all order-related use cases. Receives Command objects,
 * dispatches them, and records each execution in the command log.
 * Layer: Business Logic (Manager).
 */
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

    // -----------------------------------------------------------------------
    // Use case 1: Submit an order
    // -----------------------------------------------------------------------
    public Order submitOrder(OrderType type, String patientName, String clinician,
                             String description, OrderPriority priority) {
        Order order = orderFactory.createOrder(type, patientName, clinician, description, priority);
        OrderCommand cmd = new SubmitOrderCommand(order, orderAccess, triagingEngine, eventPublisher);
        dispatch(cmd);
        return order;
    }

    // -----------------------------------------------------------------------
    // Use case 2: Fulfil an order (claim + complete)
    // -----------------------------------------------------------------------
    public void claimOrder(String orderId, String staffMember) {
        OrderCommand cmd = new ClaimOrderCommand(orderId, staffMember, orderAccess, eventPublisher);
        dispatch(cmd);
    }

    public void completeOrder(String orderId, String staffMember) {
        OrderCommand cmd = new CompleteOrderCommand(orderId, staffMember, orderAccess, eventPublisher);
        dispatch(cmd);
    }

    // -----------------------------------------------------------------------
    // Cancel (clinician)
    // -----------------------------------------------------------------------
    public void cancelOrder(String orderId, String clinician) {
        OrderCommand cmd = new CancelOrderCommand(orderId, clinician, orderAccess, eventPublisher);
        dispatch(cmd);
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------
    public List<Order> getQueue() {
        return triagingEngine.getAllOrders();
    }

    public List<CommandLogEntry> getCommandLog() {
        return commandLogAccess.getAll();
    }

    // -----------------------------------------------------------------------
    // Internal dispatcher — executes the command and records it in the log
    // -----------------------------------------------------------------------
    private void dispatch(OrderCommand cmd) {
        cmd.execute();
        commandLogAccess.append(new CommandLogEntry(cmd.getCommandType(), cmd.getOrderId(), cmd.getActor()));
    }
}
