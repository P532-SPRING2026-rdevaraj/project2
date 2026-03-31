package com.hospital.ordersystem.client;

import com.hospital.ordersystem.manager.OrderManager;
import com.hospital.ordersystem.model.CommandLogEntry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Client layer — exposes the command audit log.
 * Delegates immediately to OrderManager; contains no business logic.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final OrderManager orderManager;

    public AuditController(OrderManager orderManager) {
        this.orderManager = orderManager;
    }

    @GetMapping
    public List<AuditDto> getAuditLog() {
        return orderManager.getCommandLog().stream().map(AuditDto::from).toList();
    }

    public record AuditDto(LocalDateTime timestamp, String commandType,
                           String orderId, String actor) {
        public static AuditDto from(CommandLogEntry e) {
            return new AuditDto(e.getTimestamp(), e.getCommandType(),
                    e.getOrderId(), e.getActor());
        }
    }
}
