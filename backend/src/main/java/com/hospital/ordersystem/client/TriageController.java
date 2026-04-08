package com.hospital.ordersystem.client;

import com.hospital.ordersystem.manager.OrderManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Client layer — exposes triage strategy selection to the UI.
 * Contains no business logic; delegates immediately to OrderManager.
 */
@RestController
@RequestMapping("/api/triage")
public class TriageController {

    private final OrderManager orderManager;

    public TriageController(OrderManager orderManager) {
        this.orderManager = orderManager;
    }

    @PutMapping("/strategy")
    public ResponseEntity<?> setStrategy(@RequestBody StrategyRequest req) {
        try {
            orderManager.setTriageStrategy(req.strategy());
            return ResponseEntity.ok(Map.of("message", "Triage strategy set to " + req.strategy()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/strategy")
    public ResponseEntity<?> getStrategy() {
        return ResponseEntity.ok(Map.of("strategy", orderManager.getTriageStrategyName()));
    }

    public record StrategyRequest(String strategy) {}
}
