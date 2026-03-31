package com.hospital.ordersystem.model;

import java.time.LocalDateTime;

public class CommandLogEntry {

    private final LocalDateTime timestamp;
    private final String commandType;
    private final String orderId;
    private final String actor;

    public CommandLogEntry(String commandType, String orderId, String actor) {
        this.timestamp = LocalDateTime.now();
        this.commandType = commandType;
        this.orderId = orderId;
        this.actor = actor;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public String getCommandType()      { return commandType; }
    public String getOrderId()          { return orderId; }
    public String getActor()            { return actor; }
}
