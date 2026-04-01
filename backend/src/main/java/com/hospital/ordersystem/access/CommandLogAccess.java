package com.hospital.ordersystem.access;

import com.hospital.ordersystem.model.CommandLogEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class CommandLogAccess {

    private final List<CommandLogEntry> log = Collections.synchronizedList(new ArrayList<>());

    public void append(CommandLogEntry entry) {
        log.add(entry);
    }

    public List<CommandLogEntry> getAll() {
        return new ArrayList<>(log);
    }
}
