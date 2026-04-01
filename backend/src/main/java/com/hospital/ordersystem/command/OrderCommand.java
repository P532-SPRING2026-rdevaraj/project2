package com.hospital.ordersystem.command;

public interface OrderCommand {
    void execute();
    default void undo() {}
    String getCommandType();
    String getOrderId();
    String getActor();
}
