package com.hospital.ordersystem.client;

import com.hospital.ordersystem.utility.InAppNotificationService;
import com.hospital.ordersystem.utility.NotificationPreferences;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationPreferences prefs;
    private final InAppNotificationService inApp;

    public NotificationController(NotificationPreferences prefs,
                                  InAppNotificationService inApp) {
        this.prefs = prefs;
        this.inApp = inApp;
    }

    @GetMapping("/preferences")
    public ResponseEntity<?> getPreferences() {
        return ResponseEntity.ok(Map.of(
                "console", prefs.isConsoleEnabled(),
                "inApp",   prefs.isInAppEnabled(),
                "email",   prefs.isEmailEnabled()));
    }

    @PutMapping("/preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody PreferencesRequest req) {
        prefs.setConsoleEnabled(req.console());
        prefs.setInAppEnabled(req.inApp());
        prefs.setEmailEnabled(req.email());
        return ResponseEntity.ok(Map.of("message", "Preferences updated."));
    }

    @GetMapping("/badge")
    public ResponseEntity<?> getBadge() {
        return ResponseEntity.ok(Map.of("count", inApp.getBadgeCount()));
    }

    @PostMapping("/badge/reset")
    public ResponseEntity<?> resetBadge() {
        inApp.resetBadgeCount();
        return ResponseEntity.ok(Map.of("message", "Badge reset."));
    }

    public record PreferencesRequest(boolean console, boolean inApp, boolean email) {}
}
