package com.hospital.ordersystem.client;

import com.hospital.ordersystem.manager.OrderManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Client layer: REST endpoints for staff roster management and on-demand
 * auto-assignment of pending orders (used when load-balancing triage is active).
 */
@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final OrderManager orderManager;

    public StaffController(OrderManager orderManager) {
        this.orderManager = orderManager;
    }

    @GetMapping
    public List<String> getAllStaff() {
        return orderManager.listAllStaff();
    }

    @PostMapping
    public ResponseEntity<?> addStaff(@RequestBody StaffRequest req) {
        if (req.staffId() == null || req.staffId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Staff ID cannot be blank."));
        }
        orderManager.addStaff(req.staffId().trim());
        return ResponseEntity.ok(Map.of("message", "Staff member '" + req.staffId().trim() + "' registered."));
    }

    @DeleteMapping("/{staffId}")
    public ResponseEntity<?> removeStaff(@PathVariable String staffId) {
        orderManager.removeStaff(staffId);
        return ResponseEntity.ok(Map.of("message", "Staff member '" + staffId + "' removed."));
    }

    @PostMapping("/auto-assign")
    public ResponseEntity<?> autoAssignPending() {
        try {
            int count = orderManager.autoAssignPendingOrders();
            String msg = count == 0
                    ? "No pending orders to assign (or no staff registered)."
                    : "Auto-assigned " + count + " pending order(s) to staff.";
            return ResponseEntity.ok(Map.of("message", msg, "assigned", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public record StaffRequest(String staffId) {}
}
