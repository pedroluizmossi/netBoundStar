package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.core.bus.TrafficBridge;
import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.view.physics.PhysicsEngine;
import com.pedro.netboundstar.view.util.StatsManager;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Custom Canvas for rendering the network visualization.
 * Handles the animation loop, state updates, and rendering of stars, particles, and HUD.
 */
public class NetworkCanvas extends Canvas {

    private final GraphicsContext gc;
    private final TrafficBridge bridge;

    private final Map<String, StarNode> stars = new HashMap<>();

    /**
     * Controls the "swelling" of the center core when traffic is detected.
     */
    private double centerHeat = 0.0;

    /**
     * Physics engine for node movement.
     */
    private final PhysicsEngine physics = new PhysicsEngine();

    // Mouse tracking for hover and click interactions
    private double mouseX = -1000;
    private double mouseY = -1000;
    private StarNode hoveredNode = null;

    /**
     * Traffic statistics manager.
     */
    private final StatsManager stats = new StatsManager();

    /**
     * Constructs a new NetworkCanvas with the specified dimensions.
     *
     * @param width  The width of the canvas.
     * @param height The height of the canvas.
     */
    public NetworkCanvas(double width, double height) {
        super(width, height);
        this.gc = this.getGraphicsContext2D();
        this.bridge = TrafficBridge.getInstance();

        // Mouse movement listener (Hover)
        this.setOnMouseMoved(e -> {
            this.mouseX = e.getX();
            this.mouseY = e.getY();
        });

        // Mouse click listener (Freeze/Unfreeze)
        this.setOnMouseClicked(e -> {
            for (StarNode node : stars.values()) {
                if (node.contains(e.getX(), e.getY())) {
                    node.isFrozen = !node.isFrozen;
                    break;
                }
            }
        });

        startLoop();
    }

    /**
     * Starts the animation loop.
     */
    private void startLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateState();
                render();
            }
        }.start();
    }

    /**
     * Determines if a packet is inbound based on the source IP.
     *
     * @param sourceIp The source IP address.
     * @return true if inbound, false if outbound.
     */
    private boolean isInbound(String sourceIp) {
        // If source IP is NOT local, it's inbound traffic.
        return !sourceIp.startsWith("192.168.") && !sourceIp.startsWith("10.") && !sourceIp.equals("127.0.0.1");
    }

    /**
     * Updates the state of all elements in the visualization.
     */
    private void updateState() {
        double centerX = getWidth() / 2;
        double centerY = getHeight() / 2;

        AppConfig config = AppConfig.get();

        PacketEvent event;
        while ((event = bridge.poll()) != null) {
            // Increase center heat when a packet arrives
            centerHeat = Math.min(centerHeat + config.getCenterHeatIncrement(), config.getCenterHeatMax());

            boolean inbound = isInbound(event.sourceIp());

            // If inbound, the remote is the source. If outbound, the remote is the target.
            String remoteIp = inbound ? event.sourceIp() : event.targetIp();

            StarNode node = stars.get(remoteIp);
            if (node == null) {
                node = new StarNode(remoteIp, centerX, centerY);
                stars.put(remoteIp, node);
            }

            // Update traffic statistics
            stats.process(event, inbound);

            // Pulse the node with the packet event
            node.pulse(event, inbound);
        }

        // Update stats tick (calculates speed every second)
        stats.tick();

        // Center elastic effect: decay heat back to 0
        centerHeat *= config.getCenterHeatDecay();

        // 1. Apply physical forces and move nodes
        physics.update(stars.values(), centerX, centerY);

        // Hover logic - update isHovered state for all nodes
        hoveredNode = null;
        for (StarNode node : stars.values()) {
            if (node.contains(mouseX, mouseY)) {
                node.isHovered = true;
                hoveredNode = node;
            } else {
                node.isHovered = false;
            }
        }

        // 2. Update logical states (decay, particles, death)
        Iterator<Map.Entry<String, StarNode>> it = stars.entrySet().iterator();
        while (it.hasNext()) {
            StarNode star = it.next().getValue();
            star.update();

            if (star.isDead()) {
                it.remove();
            }
        }
    }

    /**
     * Renders all elements to the canvas.
     */
    private void render() {
        double w = getWidth();
        double h = getHeight();
        double centerX = w / 2;
        double centerY = h / 2;

        // Clear screen
        gc.setFill(Color.rgb(10, 10, 15));
        gc.fillRect(0, 0, w, h);

        // 1. Draw connection lines (Background layer)
        gc.setLineWidth(1.0);
        for (StarNode star : stars.values()) {
            gc.setStroke(Color.rgb(100, 200, 255, Math.max(0, star.activity * 0.2)));
            gc.strokeLine(centerX, centerY, star.x, star.y);
        }

        // 2. Draw particles (Middle layer)
        for (StarNode star : stars.values()) {
            star.drawParticles(gc, centerX, centerY);
        }

        // 3. Draw stars and text (IP or Hostname) with indicators
        gc.setFont(new Font("Consolas", 12));
        for (StarNode star : stars.values()) {
            double opacity = Math.max(0, star.activity);

            // Frozen indicator (Cyan circle)
            if (star.isFrozen) {
                gc.setStroke(Color.CYAN);
                gc.setLineWidth(2);
                gc.strokeOval(star.x - 10, star.y - 10, 20, 20);
            }

            // Hover indicator (Yellow circle)
            if (star.isHovered) {
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(2);
                gc.strokeOval(star.x - 8, star.y - 8, 16, 16);
            }

            // Node drawing - with flag support (GEO)
            if (star.flagImage != null && star.flagImage.getWidth() > 0) {
                double size = 20;
                double halfSize = size / 2;
                double flagOpacity = Math.max(0.4, star.activity);

                gc.save();
                gc.setGlobalAlpha(flagOpacity);

                // Circular clipping for rounded flags
                gc.beginPath();
                gc.arc(star.x, star.y, halfSize, halfSize, 0, 360);
                gc.closePath();
                gc.clip();

                gc.drawImage(star.flagImage, star.x - halfSize, star.y - halfSize, size, size);

                gc.restore();

                // Optional: circular border around the flag
                gc.setStroke(Color.rgb(255, 255, 255, flagOpacity * 0.5));
                gc.setLineWidth(1);
                gc.strokeOval(star.x - halfSize, star.y - halfSize, size, size);
            } else {
                // Fallback: simple white dot
                gc.setFill(Color.rgb(255, 255, 255, opacity));
                gc.fillOval(star.x - 4, star.y - 4, 8, 8);
            }

            // Draw IP or Hostname if the star is active enough
            if (star.activity > 0.2) {
                gc.setFill(Color.rgb(200, 200, 200, opacity));
                gc.fillText(star.displayName, star.x + 10, star.y + 4);
            }
        }

        // 4. Pulsing Core
        double currentRadius = 30 + centerHeat;

        // Outer glow
        gc.setGlobalAlpha(0.3);
        gc.setFill(Color.CYAN);
        gc.fillOval(centerX - currentRadius, centerY - currentRadius, currentRadius * 2, currentRadius * 2);

        // Solid inner core
        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.WHITE);
        double coreRadius = 5 + (centerHeat * 0.2);
        gc.fillOval(centerX - coreRadius, centerY - coreRadius, coreRadius * 2, coreRadius * 2);

        // Top HUD
        gc.setFill(Color.LIME);
        gc.setFont(new Font("Consolas", 14));
        gc.fillText("Active Connections: " + stars.size() + " | Packets: " + stats.getTotalPackets(), 20, 30);

        // Bottom dashboard with statistics
        drawDashboard();

        // Tooltip (Rendered on top)
        if (hoveredNode != null) {
            drawTooltip(hoveredNode);
        }
    }

    /**
     * Draws the statistics dashboard at the bottom of the canvas.
     */
    private void drawDashboard() {
        double w = getWidth();
        double h = getHeight();
        double hudHeight = 100;
        double yStart = h - hudHeight;

        // 1. Panel Background (Dark glass)
        gc.setFill(Color.rgb(10, 15, 20, 0.85));
        gc.fillRect(0, yStart, w, hudHeight);

        // Neon separation line
        gc.setStroke(Color.rgb(0, 255, 255, 0.5));
        gc.setLineWidth(1);
        gc.strokeLine(0, yStart, w, yStart);

        // 2. Speedometers
        gc.setFont(new Font("Consolas", 14));

        // Download (Cyan)
        gc.setFill(Color.CYAN);
        gc.fillText("▼ DOWNLOAD", 20, yStart + 25);
        gc.setFont(new Font("Consolas", 22));
        gc.fillText(formatSpeed(stats.getDownSpeed()), 20, yStart + 50);

        // Upload (Orange)
        gc.setFont(new Font("Consolas", 14));
        gc.setFill(Color.ORANGE);
        gc.fillText("▲ UPLOAD", 180, yStart + 25);
        gc.setFont(new Font("Consolas", 22));
        gc.fillText(formatSpeed(stats.getUpSpeed()), 180, yStart + 50);

        // Totals
        gc.setFont(new Font("Consolas", 11));
        gc.setFill(Color.GRAY);
        gc.fillText("Total: " + formatBytes(stats.getTotalBytesDown()), 20, yStart + 75);
        gc.fillText("Total: " + formatBytes(stats.getTotalBytesUp()), 180, yStart + 75);

        // 3. History Graph
        drawGraph(350, yStart + 10, w - 370, hudHeight - 20);
    }

    /**
     * Draws a history graph for traffic.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param w The width.
     * @param h The height.
     */
    private void drawGraph(double x, double y, double w, double h) {
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillRect(x, y, w, h);
        gc.setStroke(Color.rgb(50, 50, 50));
        gc.strokeRect(x, y, w, h);

        var history = stats.getHistory();
        if (history.size() < 2) return;

        // Find peak for scaling
        long maxVal = 1;
        for (long[] point : history) {
            maxVal = Math.max(maxVal, Math.max(point[0], point[1]));
        }

        double stepX = w / (double) (history.size() - 1);

        // Draw Download Line (Cyan)
        gc.setStroke(Color.CYAN);
        gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i < history.size(); i++) {
            double val = history.get(i)[0];
            double px = x + (i * stepX);
            double py = y + h - ((val / (double) maxVal) * h);
            if (i == 0) gc.moveTo(px, py);
            else gc.lineTo(px, py);
        }
        gc.stroke();

        // Draw Upload Line (Orange)
        gc.setStroke(Color.ORANGE);
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int i = 0; i < history.size(); i++) {
            double val = history.get(i)[1];
            double px = x + (i * stepX);
            double py = y + h - ((val / (double) maxVal) * h);
            if (i == 0) gc.moveTo(px, py);
            else gc.lineTo(px, py);
        }
        gc.stroke();

        // Scale (max value)
        gc.setFill(Color.GRAY);
        gc.setFont(new Font("Consolas", 9));
        gc.fillText(formatBytes(maxVal) + "/s", x + 5, y + 12);
    }

    /**
     * Formats speed in bytes per second to a human-readable string.
     *
     * @param bytesPerSec The speed in bytes per second.
     * @return Formatted speed string.
     */
    private String formatSpeed(long bytesPerSec) {
        return formatBytes(bytesPerSec) + "/s";
    }

    /**
     * Formats bytes to a human-readable string (e.g., KB, MB).
     *
     * @param bytes The number of bytes.
     * @return Formatted byte string.
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Draws a tooltip with detailed information about a node.
     *
     * @param node The StarNode to show information for.
     */
    private void drawTooltip(StarNode node) {
        double bx = node.x + 20;
        double by = node.y - 20;
        double bw = 240;
        double bh = 100;

        // Semi-transparent background
        gc.setFill(Color.rgb(20, 20, 30, 0.95));
        gc.setStroke(Color.CYAN);
        gc.setLineWidth(1.5);
        gc.fillRect(bx, by, bw, bh);
        gc.strokeRect(bx, by, bw, bh);

        // Information text
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Consolas", 10));
        gc.fillText("Host:  " + node.displayName, bx + 10, by + 18);
        gc.fillText("IP:    " + node.ip, bx + 10, by + 33);
        gc.fillText("Ports: " + node.lastPorts, bx + 10, by + 48);
        gc.fillText("Status: " + (node.isFrozen ? "FROZEN ❄" : "Free"), bx + 10, by + 63);

        // Highlighted data
        gc.setFill(Color.LIME);
        gc.fillText("Total: " + formatBytes(node.totalBytes), bx + 10, by + 83);
    }

    @Override
    public boolean isResizable() { return true; }
    @Override
    public double prefWidth(double h) { return getWidth(); }
    @Override
    public double prefHeight(double w) { return getHeight(); }
}
