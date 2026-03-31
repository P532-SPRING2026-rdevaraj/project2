package com.hospital.ordersystem.utility;

import com.hospital.ordersystem.model.Order;

/**
 * Utility — cross-cutting notification channel. Decouples the notification
 * mechanism from business logic so implementations can be swapped
 * (console, email, WebSocket) without touching the Manager or Observer.
 * Layer: Utility.
 */
public interface NotificationService {
    void notify(Order order, String event);
}
