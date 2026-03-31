package com.hospital.ordersystem.model;

public enum OrderPriority {
    ROUTINE(1),
    URGENT(2),
    STAT(3);

    private final int level;

    OrderPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
