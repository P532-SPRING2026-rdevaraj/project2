package com.hospital.ordersystem.manager;

import com.hospital.ordersystem.access.CommandLogAccess;
import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.command.*;
import com.hospital.ordersystem.engine.TriagingEngine;
import com.hospital.ordersystem.factory.OrderFactory;
import com.hospital.ordersystem.model.*;
import com.hospital.ordersystem.observer.OrderEventPublisher;
import com.hospital.ordersystem.strategy.TriageStrategy;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Map;

@Component
public class OrderManager {

    private final OrderFactory orderFactory;
    private final OrderAccess orderAccess;
    private final CommandLogAccess commandLogAccess;
    private final TriagingEngine triagingEngine;
    private final OrderEventPublisher eventPublisher;
    private final Clock clock;
    private final Map<String, TriageStrategy> strategies;

    // Single-level undo stack (spec: "only the most recent command")
    private OrderCommand lastCommand;
    private String currentStrategyName = "PRIORITY_FIRST";

    public OrderManager(OrderFactory orderFactory,
                        OrderAccess orderAccess,
                        CommandLogAccess commandLogAccess,
                        TriagingEngine triagingEngine,
                        OrderEventPublisher eventPublisher,
                        Clock clock,
                        Map<String, TriageStrategy> strategies) {
        this.orderFactory = orderFactory;
        this.orderAccess = orderAccess;
        this.commandLogAccess = commandLogAccess;
        this.triagingEngine = triagingEngine;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.strategies = strategies;
    }

    // ── Core use-case 1: Submit ───────────────────────────────────────────────

    public Order submitOrder(OrderType type, String patientName, String clinician,
                             String description, OrderPriority priority) {
        Order order = orderFactory.createOrder(type, patientName, clinician, description, priority);
        dispatch(new StatAwareSubmitOrderCommand(order, orderAccess, triagingEngine,
                eventPublisher, clock, commandLogAccess));
        return order;
    }

    // ── Core use-case 2: Fulfil ───────────────────────────────────────────────

    public void claimOrder(String orderId, String staffMember) {
        dispatch(new ClaimOrderCommand(orderId, staffMember, orderAccess, eventPublisher));
    }

    public void completeOrder(String orderId, String staffMember) {
        dispatch(new CompleteOrderCommand(orderId, staffMember, orderAccess, eventPublisher));
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    public void cancelOrder(String orderId, String clinician) {
        dispatch(new CancelOrderCommand(orderId, clinician, orderAccess, eventPublisher));
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    public List<Order> getQueue() {
        return triagingEngine.getAllOrders();
    }

    public List<CommandLogEntry> getCommandLog() {
        return commandLogAccess.getAll();
    }

    // ── Change 1: Triage strategy selection ──────────────────────────────────

    public void setTriageStrategy(String strategyName) {
        String key = strategyName.toUpperCase();
        TriageStrategy strategy = switch (key) {
            case "PRIORITY_FIRST" -> strategies.get("priorityFirstTriageStrategy");
            case "LOAD_BALANCING" -> strategies.get("LOAD_BALANCING");
            case "DEADLINE_FIRST" -> strategies.get("DEADLINE_FIRST");
            default -> throw new IllegalArgumentException(
                    "Unknown triage strategy: " + strategyName +
                    ". Valid values: PRIORITY_FIRST, LOAD_BALANCING, DEADLINE_FIRST");
        };
        triagingEngine.setTriageStrategy(strategy);
        currentStrategyName = key;
    }

    public String getTriageStrategyName() {
        return currentStrategyName;
    }

    // ── Change 3: Undo ────────────────────────────────────────────────────────

    public void undoLastCommand() {
        if (lastCommand == null) {
            throw new IllegalStateException("No command to undo.");
        }
        lastCommand.undo();
        commandLogAccess.append(
                new CommandLogEntry("UNDO", lastCommand.getOrderId(), lastCommand.getActor()));
        lastCommand = null;
    }

    // ── Change 3: Replay ─────────────────────────────────────────────────────

    public void replayCommand(int index) {
        List<CommandLogEntry> log = commandLogAccess.getAll();
        if (index < 0 || index >= log.size()) {
            throw new IllegalArgumentException("Invalid log index: " + index);
        }
        CommandLogEntry entry = log.get(index);
        OrderCommand cmd = switch (entry.getCommandType()) {
            case "SUBMIT" -> {
                Order order = orderAccess.findOrderById(entry.getOrderId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Order not found for replay: " + entry.getOrderId()));
                order.setStatus(OrderStatus.PENDING);
                order.setClaimedBy(null);
                yield new StatAwareSubmitOrderCommand(order, orderAccess, triagingEngine,
                        eventPublisher, clock, commandLogAccess);
            }
            case "CANCEL"   -> new CancelOrderCommand(entry.getOrderId(), entry.getActor(),
                                    orderAccess, eventPublisher);
            case "CLAIM"    -> new ClaimOrderCommand(entry.getOrderId(), entry.getActor(),
                                    orderAccess, eventPublisher);
            case "COMPLETE" -> new CompleteOrderCommand(entry.getOrderId(), entry.getActor(),
                                    orderAccess, eventPublisher);
            default -> throw new IllegalArgumentException(
                    "Cannot replay command type: " + entry.getCommandType());
        };
        dispatch(cmd);
    }

    // ── Internal dispatch ─────────────────────────────────────────────────────

    private void dispatch(OrderCommand cmd) {
        // Wrap every command in UndoableCommandDecorator so undo() works
        // without modifying any individual command class.
        UndoableCommandDecorator undoable = new UndoableCommandDecorator(cmd, orderAccess);
        undoable.execute();
        commandLogAccess.append(
                new CommandLogEntry(cmd.getCommandType(), cmd.getOrderId(), cmd.getActor()));
        lastCommand = undoable;
    }
}
