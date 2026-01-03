package com.pedro.netboundstar.engine.util;

import com.pedro.netboundstar.core.health.AppHealth;
import com.pedro.netboundstar.core.health.CapturePrivileges;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Best-effort Linux permission/capability checks for packet capture.
 *
 * Packet capture typically requires one of:
 * - running as root
 * - CAP_NET_RAW (and often CAP_NET_ADMIN) on the java binary / process
 */
public final class CapturePermissionChecker {

    private CapturePermissionChecker() {
    }

    public static void checkAndPublishStatus() {
        // Non-Linux: we can't check capabilities reliably here.
        if (!isLinux()) {
            AppHealth.setCapturePrivileges(CapturePrivileges.UNKNOWN,
                    "Packet capture may require Administrator/root privileges.");
            return;
        }

        // Root is always OK.
        if (isRoot()) {
            AppHealth.setCapturePrivileges(CapturePrivileges.OK, null);
            return;
        }

        // Best-effort: resolve the real java binary and check its file capabilities.
        // Many distros have /usr/bin/java as a symlink (setcap on symlink fails with:
        // "Invalid file '/usr/bin/java' for capability operation").
        String javaPath = resolveJavaBinaryPath();
        if (javaPath == null) {
            AppHealth.setCapturePrivileges(CapturePrivileges.UNKNOWN,
                    "Couldn't locate 'java' to check capabilities. Capture may fail unless run with sudo.");
            return;
        }

        String caps = getCap(javaPath);
        if (caps == null) {
            AppHealth.setCapturePrivileges(CapturePrivileges.UNKNOWN,
                    "Couldn't check Linux capabilities (getcap not available). Run with sudo, or setcap CAP_NET_RAW on the real java binary (not /usr/bin/java symlink)." +
                    " Example: sudo setcap cap_net_raw,cap_net_admin=eip '" + javaPath + "'");
            return;
        }

        String lower = caps.toLowerCase();
        boolean hasNetRaw = lower.contains("cap_net_raw");
        boolean hasNetAdmin = lower.contains("cap_net_admin");

        if (hasNetRaw) {
            String msg = hasNetAdmin ? null : "CAP_NET_RAW is present but CAP_NET_ADMIN is missing; capture may still fail on some interfaces.";
            AppHealth.setCapturePrivileges(CapturePrivileges.OK, msg);
        } else {
            AppHealth.setCapturePrivileges(CapturePrivileges.INSUFFICIENT,
                    "No capture privileges detected. Run with sudo, or grant capabilities on the real java binary (symlinks won't work). Example: sudo setcap cap_net_raw,cap_net_admin=eip '" + javaPath + "'");
        }
    }

    private static boolean isLinux() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("linux");
    }

    private static boolean isRoot() {
        String user = System.getProperty("user.name");
        return "root".equals(user);
    }

    private static String which(String cmd) {
        try {
            Process p = new ProcessBuilder("sh", "-lc", "command -v " + cmd).redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String out = br.readLine();
                int code = p.waitFor();
                if (code == 0 && out != null && !out.isBlank()) {
                    return out.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String getCap(String path) {
        try {
            Process p = new ProcessBuilder("sh", "-lc", "getcap " + quote(path)).redirectErrorStream(true).start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            int code = p.waitFor();
            if (code != 0) {
                return null;
            }
            String out = sb.toString().trim();
            return out.isEmpty() ? "" : out;
        } catch (Exception e) {
            return null;
        }
    }

    private static String quote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    private static String resolveJavaBinaryPath() {
        // Prefer resolving what the shell would execute, and then de-symlink it.
        String javaCmd = which("java");
        if (javaCmd == null) return null;

        // Resolve symlinks via readlink -f
        try {
            Process p = new ProcessBuilder("sh", "-lc", "readlink -f " + quote(javaCmd)).redirectErrorStream(true).start();
            String out;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                out = br.readLine();
            }
            int code = p.waitFor();
            if (code == 0 && out != null && !out.isBlank()) {
                return out.trim();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // Best-effort only; fall back.
        }

        return javaCmd;
    }
}
