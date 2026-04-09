package com.hospital.ordersystem.utility;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class NotificationPreferences {

    private final AtomicBoolean consoleEnabled = new AtomicBoolean(true);
    private final AtomicBoolean inAppEnabled   = new AtomicBoolean(true);
    private final AtomicBoolean emailEnabled   = new AtomicBoolean(false);

    public boolean isConsoleEnabled() { return consoleEnabled.get(); }
    public boolean isInAppEnabled()   { return inAppEnabled.get(); }
    public boolean isEmailEnabled()   { return emailEnabled.get(); }

    public void setConsoleEnabled(boolean v) { consoleEnabled.set(v); }
    public void setInAppEnabled(boolean v)   { inAppEnabled.set(v); }
    public void setEmailEnabled(boolean v)   { emailEnabled.set(v); }
}
