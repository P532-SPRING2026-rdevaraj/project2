package com.hospital.ordersystem.command;

/**
 * Command pattern — encapsulates each order action (submit, claim, complete,
 * cancel) as an object so operations can be queued, logged, and undone.
 * Layer: Business Logic.
 */
public interface OrderCommand {
    void execute();

    /**
     * Reverses the effect of execute(). Default is no-op so existing
     * commands compile without change until Week 2 implements undo logic.
     */
    default void undo() {}

    String getCommandType();
    String getOrderId();
    String getActor();
}
