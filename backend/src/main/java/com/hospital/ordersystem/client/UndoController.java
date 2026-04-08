package com.hospital.ordersystem.client;

import com.hospital.ordersystem.manager.OrderManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Client layer — exposes the Command undo and replay operations.
 * Contains no business logic; delegates immediately to OrderManager.
 */
@RestController
@RequestMapping("/api/orders")
public class UndoController {

    private final OrderManager orderManager;

    public UndoController(OrderManager orderManager) {
        this.orderManager = orderManager;
    }

    /** Undo the most recently executed command (single-level undo). */
    @PostMapping("/undo")
    public ResponseEntity<?> undoLastCommand() {
        try {
            orderManager.undoLastCommand();
            return ResponseEntity.ok(Map.of("message", "Last command undone."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Re-execute a past command from the audit log by its 0-based index. */
    @PostMapping("/replay/{logIndex}")
    public ResponseEntity<?> replayCommand(@PathVariable int logIndex) {
        try {
            orderManager.replayCommand(logIndex);
            return ResponseEntity.ok(Map.of("message", "Command replayed."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
