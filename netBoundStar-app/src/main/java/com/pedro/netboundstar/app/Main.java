package com.pedro.netboundstar.app;

import com.pedro.netboundstar.engine.service.SnifferService;
import com.pedro.netboundstar.engine.util.CapturePermissionChecker;
import com.pedro.netboundstar.view.StarViewApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the NetBoundStar application.
 * Initializes the capture engine and launches the graphical interface.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Main method.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        logger.info("╔════════════════════════════════════════════════════════════╗");
        logger.info("║           NetBoundStar - Starting                          ║");
        logger.info("║        Network Traffic Visualization Engine                ║");
        logger.info("╚════════════════════════════════════════════════════════════╝");

        // Check capture permissions/capabilities early so the UI can show a friendly warning.
        CapturePermissionChecker.checkAndPublishStatus();

        // 1. Start the Sniffer in the background (Daemon Thread)
        // Daemon threads automatically terminate when the main application closes.
        logger.info("▶ Starting capture engine...");
        SnifferService sniffer = new SnifferService();
        Thread captureThread = new Thread(sniffer, "Sniffer-Thread");
        captureThread.setDaemon(true);
        captureThread.start();
        logger.info("✓ Sniffer started in background.");

        // 2. Launch the Graphical Interface (Blocks the main thread until the window is closed)
        logger.info("▶ Opening window...");
        StarViewApp.launchApp();
    }
}
