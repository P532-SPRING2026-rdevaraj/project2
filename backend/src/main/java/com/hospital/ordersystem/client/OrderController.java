package com.hospital.ordersystem.client;

import com.hospital.ordersystem.manager.OrderManager;
import com.hospital.ordersystem.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Client layer — REST API surface. Contains zero business logic; every
 * request is immediately delegated to OrderManager.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderManager orderManager;

    public OrderController(OrderManager orderManager) {
        this.orderManager = orderManager;
    }

    /** GET /api/orders — full queue (all statuses, triage-sorted) */
    @GetMapping
    public List<OrderDto> getQueue() {
        return orderManager.getQueue().stream().map(OrderDto::from).toList();
    }

    /** POST /api/orders — submit a new order */
    @PostMapping
    public ResponseEntity<?> submitOrder(@RequestBody SubmitRequest req) {
        try {
            OrderType type = OrderType.valueOf(req.type().toUpperCase());
            OrderPriority priority = OrderPriority.valueOf(req.priority().toUpperCase());
            Order order = orderManager.submitOrder(type, req.patientName(),
                    req.clinician(), req.description(), priority);
            return ResponseEntity.ok(OrderDto.from(order));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/orders/{id}/claim */
    @PostMapping("/{id}/claim")
    public ResponseEntity<?> claimOrder(@PathVariable String id,
                                        @RequestBody ActorRequest req) {
        try {
            orderManager.claimOrder(id, req.actor());
            return ResponseEntity.ok(Map.of("message", "Order claimed."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/orders/{id}/complete */
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeOrder(@PathVariable String id,
                                           @RequestBody ActorRequest req) {
        try {
            orderManager.completeOrder(id, req.actor());
            return ResponseEntity.ok(Map.of("message", "Order completed."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/orders/{id}/cancel */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String id,
                                         @RequestBody ActorRequest req) {
        try {
            orderManager.cancelOrder(id, req.actor());
            return ResponseEntity.ok(Map.of("message", "Order cancelled."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // -----------------------------------------------------------------------
    // DTOs and request records
    // -----------------------------------------------------------------------

    public record SubmitRequest(String type, String patientName, String clinician,
                                String description, String priority) {}

    public record ActorRequest(String actor) {}

    public record OrderDto(String orderId, String type, String patientName,
                           String clinician, String description,
                           String priority, String status,
                           String claimedBy, LocalDateTime submittedAt) {
        public static OrderDto from(Order o) {
            return new OrderDto(o.getOrderId(), o.getType().name(),
                    o.getPatientName(), o.getClinician(), o.getDescription(),
                    o.getPriority().name(), o.getStatus().name(),
                    o.getClaimedBy(), o.getSubmittedAt());
        }
    }
}
