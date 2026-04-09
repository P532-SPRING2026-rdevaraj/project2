package com.hospital.ordersystem.client;

import com.hospital.ordersystem.manager.OrderManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class UndoController {

    private final OrderManager orderManager;

    public UndoController(OrderManager orderManager) {
        this.orderManager = orderManager;
    }

    @PostMapping("/undo")
    public ResponseEntity<?> undoLastCommand() {
        try {
            orderManager.undoLastCommand();
            return ResponseEntity.ok(Map.of("message", "Last command undone."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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
