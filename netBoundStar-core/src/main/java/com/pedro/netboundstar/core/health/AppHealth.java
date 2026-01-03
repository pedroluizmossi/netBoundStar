package com.pedro.netboundstar.core.health;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal cross-module health/status registry.
 *
 * The UI reads this to show friendly, actionable warnings (e.g. missing capture permissions).
 */
public final class AppHealth {

    private static final AtomicReference<CapturePrivileges> CAPTURE_PRIVILEGES =
            new AtomicReference<>(CapturePrivileges.UNKNOWN);

    private static final AtomicReference<String> CAPTURE_PRIVILEGES_MESSAGE =
            new AtomicReference<>(null);

    private AppHealth() {
    }

    public static CapturePrivileges getCapturePrivileges() {
        return CAPTURE_PRIVILEGES.get();
    }

    public static String getCapturePrivilegesMessage() {
        return CAPTURE_PRIVILEGES_MESSAGE.get();
    }

    public static void setCapturePrivileges(CapturePrivileges status, String message) {
        CAPTURE_PRIVILEGES.set(status != null ? status : CapturePrivileges.UNKNOWN);
        CAPTURE_PRIVILEGES_MESSAGE.set(message);
    }
}

