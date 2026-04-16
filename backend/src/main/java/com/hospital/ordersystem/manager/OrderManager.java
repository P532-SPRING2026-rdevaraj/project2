package com.hospital.ordersystem.manager;

import com.hospital.ordersystem.access.CommandLogAccess;
import com.hospital.ordersystem.access.OrderAccess;
import com.hospital.ordersystem.access.StaffAccess;
import com.hospital.ordersystem.command.*;
import com.hospital.ordersystem.engine.TriagingEngine;
import com.hospital.ordersystem.factory.OrderFactory;
import com.hospital.ordersystem.model.*;
import com.hospital.ordersystem.observer.OrderEventPublisher;
import com.hospital.ordersystem.strategy.TriageStrategy;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderManager {

    private final OrderFactory orderFactory;
    private final OrderAccess orderAccess;
    private final CommandLogAccess commandLogAccess;
    private final TriagingEngine triagingEngine;
    private final OrderEventPublisher eventPublisher;
    private final Clock clock;
    private final Map<String, TriageStrategy> strategies;
    private final StaffAccess staffAccess;

    private OrderCommand lastCommand;
    private String currentStrategyName = "PRIORITY_FIRST";

    public OrderManager(OrderFactory orderFactory,
                        OrderAccess orderAccess,
                        CommandLogAccess commandLogAccess,
                        TriagingEngine triagingEngine,
                        OrderEventPublisher eventPublisher,
                        Clock clock,
                        Map<String, TriageStrategy> strategies,
                        StaffAccess staffAccess) {
        this.orderFactory = orderFactory;
        this.orderAccess = orderAccess;
        this.commandLogAccess = commandLogAccess;
        this.triagingEngine = triagingEngine;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.strategies = strategies;
        this.staffAccess = staffAccess;
    }

    public Order submitOrder(OrderType type, String patientName, String clinician,
                             String description, OrderPriority priority) {
        Order order = orderFactory.createOrder(type, patientName, clinician, description, priority);
        dispatch(new StatAwareSubmitOrderCommand(order, orderAccess, triagingEngine,
                eventPublisher, clock, commandLogAccess));
        // Auto-assign immediately when load-balancing is active and staff is registered
        if ("LOAD_BALANCING".equals(currentStrategyName) && !staffAccess.isEmpty()) {
            String assignee = findLeastLoadedStaff(staffAccess.listAllStaff());
            dispatch(new ClaimOrderCommand(order.getOrderId(), assignee, orderAccess, eventPublisher));
        }
        return order;
    }

    // ── Staff management ─────────────────────────────────────────────────────

    public void addStaff(String staffId) {
        staffAccess.addStaff(staffId);
    }

    public void removeStaff(String staffId) {
        staffAccess.removeStaff(staffId);
    }

    public List<String> listAllStaff() {
        return staffAccess.listAllStaff();
    }

    /**
     * Assigns every PENDING order to the least-loaded staff member in sequence.
     * Used when the strategy is switched to LOAD_BALANCING mid-session with
     * already-queued orders, or triggered manually from the UI.
     *
     * @return number of orders assigned
     */
    public int autoAssignPendingOrders() {
        List<String> allStaff = staffAccess.listAllStaff();
        if (allStaff.isEmpty()) return 0;

        List<Order> pending = triagingEngine.getTriagedQueue();
        int count = 0;
        for (Order order : pending) {
            String assignee = findLeastLoadedStaff(allStaff);
            dispatch(new ClaimOrderCommand(order.getOrderId(), assignee, orderAccess, eventPublisher));
            count++;
        }
        return count;
    }

    /**
     * Returns the registered staff member with the fewest IN_PROGRESS orders.
     * Re-queries the store on each call so that workload stays accurate as
     * orders are assigned in a batch.
     */
    private String findLeastLoadedStaff(List<String> allStaff) {
        Map<String, Long> workload = orderAccess.listAllOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS && o.getClaimedBy() != null)
                .collect(Collectors.groupingBy(Order::getClaimedBy, Collectors.counting()));
        return allStaff.stream()
                .min(Comparator.comparingLong(s -> workload.getOrDefault(s, 0L)))
                .orElseThrow(() -> new IllegalStateException("No staff members registered."));
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

    public void undoLastCommand() {
        if (lastCommand == null) {
            throw new IllegalStateException("No command to undo.");
        }
        lastCommand.undo();
        commandLogAccess.append(
                new CommandLogEntry("UNDO", lastCommand.getOrderId(), lastCommand.getActor()));
        lastCommand = null;
    }

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

    private void dispatch(OrderCommand cmd) {
        UndoableCommandDecorator undoable = new UndoableCommandDecorator(cmd, orderAccess);
        undoable.execute();
        commandLogAccess.append(
                new CommandLogEntry(cmd.getCommandType(), cmd.getOrderId(), cmd.getActor()));
        lastCommand = undoable;
    }
}
