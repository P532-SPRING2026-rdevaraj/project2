package com.hospital.ordersystem.command;

/**
 * Command pattern — encapsulates each order action (submit, claim, complete,
 * cancel) as an object so operations can be queued, logged, and undone.
 * Layer: Business Logic.
 */
public interface OrderCommand {
    void execute();
    String getCommandType();
    String getOrderId();
    String getActor();
}
