package com.hospital.ordersystem;

import com.hospital.ordersystem.model.*;
import com.hospital.ordersystem.utility.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers Change 2a — CompositeNotificationService routes notifications only to
 * channels that are enabled in NotificationPreferences.
 */
@ExtendWith(MockitoExtension.class)
class NotificationChainTest {

    @Mock private ConsoleNotificationService console;
    @Mock private InAppNotificationService   inApp;
    @Mock private EmailNotificationService   email;

    private NotificationPreferences prefs;
    private CompositeNotificationService composite;
    private Order order;

    @BeforeEach
    void setUp() {
        prefs = new NotificationPreferences(); // console ON, inApp ON, email OFF by default
        composite = new CompositeNotificationService(console, inApp, email, prefs);
        order = new LabOrder("TEST-01", "Alice", "Dr. Smith", "CBC", OrderPriority.ROUTINE);
    }

    @Test
    void notify_defaultPrefs_consoleAndInAppReceiveEvent_emailDoesNot() {
        // Arrange — default prefs: console=true, inApp=true, email=false

        // Act
        composite.notify(order, "ORDER_SUBMITTED");

        // Assert
        verify(console, times(1)).notify(order, "ORDER_SUBMITTED");
        verify(inApp,   times(1)).notify(order, "ORDER_SUBMITTED");
        verify(email,   never()).notify(any(), anyString());
    }

    @Test
    void notify_emailEnabled_allThreeChannelsFire() {
        // Arrange
        prefs.setEmailEnabled(true);

        // Act
        composite.notify(order, "ORDER_CLAIMED");

        // Assert
        verify(console, times(1)).notify(order, "ORDER_CLAIMED");
        verify(inApp,   times(1)).notify(order, "ORDER_CLAIMED");
        verify(email,   times(1)).notify(order, "ORDER_CLAIMED");
    }

    @Test
    void notify_allDisabled_noChannelFires() {
        // Arrange
        prefs.setConsoleEnabled(false);
        prefs.setInAppEnabled(false);
        prefs.setEmailEnabled(false);

        // Act
        composite.notify(order, "ORDER_CANCELLED");

        // Assert
        verify(console, never()).notify(any(), anyString());
        verify(inApp,   never()).notify(any(), anyString());
        verify(email,   never()).notify(any(), anyString());
    }

    @Test
    void inAppNotificationService_incrementsBadgeCount() {
        // Arrange
        InAppNotificationService real = new InAppNotificationService();

        // Act
        real.notify(order, "ORDER_SUBMITTED");
        real.notify(order, "ORDER_CLAIMED");

        // Assert
        assertEquals(2, real.getBadgeCount());
    }

    @Test
    void inAppNotificationService_resetBadgeCount() {
        // Arrange
        InAppNotificationService real = new InAppNotificationService();
        real.notify(order, "ORDER_SUBMITTED");

        // Act
        real.resetBadgeCount();

        // Assert
        assertEquals(0, real.getBadgeCount());
    }
}
