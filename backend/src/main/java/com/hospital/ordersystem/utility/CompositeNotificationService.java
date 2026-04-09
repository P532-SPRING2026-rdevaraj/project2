package com.hospital.ordersystem.utility;

import com.hospital.ordersystem.model.Order;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class CompositeNotificationService implements NotificationService {

    private final ConsoleNotificationService console;
    private final InAppNotificationService inApp;
    private final EmailNotificationService email;
    private final NotificationPreferences prefs;

    public CompositeNotificationService(ConsoleNotificationService console,
                                        InAppNotificationService inApp,
                                        EmailNotificationService email,
                                        NotificationPreferences prefs) {
        this.console = console;
        this.inApp   = inApp;
        this.email   = email;
        this.prefs   = prefs;
    }

    @Override
    public void notify(Order order, String event) {
        if (prefs.isConsoleEnabled()) console.notify(order, event);
        if (prefs.isInAppEnabled())   inApp.notify(order, event);
        if (prefs.isEmailEnabled())   email.notify(order, event);
    }
}
