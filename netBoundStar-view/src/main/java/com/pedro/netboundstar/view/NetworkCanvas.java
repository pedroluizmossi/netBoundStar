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
 * Handles the animation loop, state updates, and rendering of stars and particles.
 */
public class NetworkCanvas extends Canvas {

    private final GraphicsContext gc;
    private final TrafficBridge bridge;

    private final Map<String, StarNode> stars = new HashMap<>();

    private double centerHeat = 0.0;
    private final PhysicsEngine physics = new PhysicsEngine();

    private double mouseX = -1000;
    private double mouseY = -1000;
    private StarNode hoveredNode = null;

    private final StatsManager stats = new StatsManager();
    private volatile boolean paused = false;

    // --- Debug Metrics ---
    private long lastFrameTime = 0;
    private double fps = 0;
    private double frameTimeMs = 0;

    public NetworkCanvas(double width, double height) {
        super(width, height);
        this.gc = this.getGraphicsContext2D();
        this.bridge = TrafficBridge.getInstance();

        this.setOnMouseMoved(e -> {
            this.mouseX = e.getX();
            this.mouseY = e.getY();
        });

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

    private void startLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Calculate FPS and Frame Time
                if (lastFrameTime > 0) {
                    long delta = now - lastFrameTime;
                    frameTimeMs = delta / 1_000_000.0;
                    fps = 1_000_000_000.0 / delta;
                }
                lastFrameTime = now;

                if (!paused) {
                    updateState();
                }
                render();
            }
        }.start();
    }

    public void setPaused(boolean paused) { this.paused = paused; }
    public boolean isPaused() { return paused; }
    public void clear() { stars.clear(); }
    public StatsManager getStats() { return stats; }

    private boolean isInbound(String sourceIp) {
        return !sourceIp.startsWith("192.168.") && !sourceIp.startsWith("10.") && !sourceIp.equals("127.0.0.1");
    }

    private void updateState() {
        double centerX = getWidth() / 2;
        double centerY = getHeight() / 2;
        AppConfig config = AppConfig.get();

        PacketEvent event;
        while ((event = bridge.poll()) != null) {
            centerHeat = Math.min(centerHeat + config.getCenterHeatIncrement(), config.getCenterHeatMax());
            boolean inbound = isInbound(event.sourceIp());
            String remoteIp = inbound ? event.sourceIp() : event.targetIp();

            StarNode node = stars.get(remoteIp);
            if (node == null) {
                node = new StarNode(remoteIp, centerX, centerY);
                stars.put(remoteIp, node);
            }
            stats.process(event, inbound);
            node.pulse(event, inbound);
        }

        stats.tick();
        centerHeat *= config.getCenterHeatDecay();
        physics.update(stars.values(), centerX, centerY);

        hoveredNode = null;
        for (StarNode node : stars.values()) {
            if (node.contains(mouseX, mouseY)) {
                node.isHovered = true;
                hoveredNode = node;
            } else {
                node.isHovered = false;
            }
        }

        Iterator<Map.Entry<String, StarNode>> it = stars.entrySet().iterator();
        while (it.hasNext()) {
            StarNode star = it.next().getValue();
            star.update();
            if (star.isDead()) it.remove();
        }
    }

    private void render() {
        double w = getWidth();
        double h = getHeight();
        double centerX = w / 2;
        double centerY = h / 2;

        gc.setFill(Color.rgb(10, 10, 15));
        gc.fillRect(0, 0, w, h);

        for (StarNode star : stars.values()) {
            Color lineColor = star.getLastProtocolColor().deriveColor(0, 1, 1, Math.max(0, star.activity * 0.2));
            gc.setStroke(lineColor);
            gc.setLineWidth(1.0);
            gc.strokeLine(centerX, centerY, star.x, star.y);
        }

        for (StarNode star : stars.values()) {
            star.drawParticles(gc, centerX, centerY);
        }

        gc.setFont(new Font("Consolas", 12));
        for (StarNode star : stars.values()) {
            double opacity = Math.max(0, star.activity);
            if (star.isFrozen) {
                gc.setStroke(Color.CYAN);
                gc.setLineWidth(2);
                gc.strokeOval(star.x - 10, star.y - 10, 20, 20);
            }
            if (star.isHovered) {
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(2);
                gc.strokeOval(star.x - 8, star.y - 8, 16, 16);
            }

            if (star.flagImage != null && star.flagImage.getWidth() > 0) {
                double size = 20;
                double halfSize = size / 2;
                double flagOpacity = Math.max(0.4, star.activity);
                gc.save();
                gc.setGlobalAlpha(flagOpacity);
                gc.beginPath();
                gc.arc(star.x, star.y, halfSize, halfSize, 0, 360);
                gc.closePath();
                gc.clip();
                gc.drawImage(star.flagImage, star.x - halfSize, star.y - halfSize, size, size);
                gc.restore();
                gc.setStroke(star.getLastProtocolColor().deriveColor(0, 1, 1, flagOpacity * 0.8));
                gc.setLineWidth(1.5);
                gc.strokeOval(star.x - halfSize, star.y - halfSize, size, size);
            } else {
                gc.setFill(star.getLastProtocolColor().deriveColor(0, 1, 1, opacity));
                gc.fillOval(star.x - 4, star.y - 4, 8, 8);
                gc.setGlobalAlpha(opacity * 0.3);
                gc.fillOval(star.x - 6, star.y - 6, 12, 12);
                gc.setGlobalAlpha(1.0);
            }

            if (star.activity > 0.2) {
                gc.setFill(Color.rgb(200, 200, 200, opacity));
                gc.fillText(star.displayName, star.x + 10, star.y + 4);
            }
        }

        double currentRadius = 30 + centerHeat;
        gc.setGlobalAlpha(0.3);
        gc.setFill(Color.CYAN);
        gc.fillOval(centerX - currentRadius, centerY - currentRadius, currentRadius * 2, currentRadius * 2);
        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.WHITE);
        double coreRadius = 5 + (centerHeat * 0.2);
        gc.fillOval(centerX - coreRadius, centerY - coreRadius, coreRadius * 2, coreRadius * 2);

        if (hoveredNode != null) drawTooltip(hoveredNode);

        // --- Render Debug Info ---
        if (AppConfig.get().isDebugMode()) {
            renderDebugInfo();
        }
    }

    private void renderDebugInfo() {
        gc.setFill(Color.rgb(0, 0, 0, 0.6));
        gc.fillRect(10, 100, 180, 80);
        gc.setStroke(Color.LIME);
        gc.setLineWidth(1);
        gc.strokeRect(10, 100, 180, 80);

        gc.setFill(Color.LIME);
        gc.setFont(new Font("Consolas", 11));
        gc.fillText(String.format("FPS: %.1f", fps), 20, 120);
        gc.fillText(String.format("Frame: %.2f ms", frameTimeMs), 20, 135);
        gc.fillText(String.format("Nodes: %d", stars.size()), 20, 150);
        gc.fillText(String.format("Queue: %d", bridge.size()), 20, 165);
        
        // Color indicator for lag
        if (frameTimeMs > 16.6) { // Below 60 FPS
            gc.setFill(Color.RED);
            gc.fillOval(160, 112, 8, 8);
        } else {
            gc.setFill(Color.LIME);
            gc.fillOval(160, 112, 8, 8);
        }
    }

    private void drawTooltip(StarNode node) {
        double bx = node.x + 20;
        double by = node.y - 20;
        double bw = 240;
        double bh = 100;
        gc.setFill(Color.rgb(20, 20, 30, 0.95));
        gc.setStroke(Color.CYAN);
        gc.setLineWidth(1.5);
        gc.fillRect(bx, by, bw, bh);
        gc.strokeRect(bx, by, bw, bh);
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Consolas", 10));
        gc.fillText("Host:  " + node.displayName, bx + 10, by + 18);
        gc.fillText("IP:    " + node.ip, bx + 10, by + 33);
        gc.fillText("Ports: " + node.lastPorts, bx + 10, by + 48);
        gc.fillText("Status: " + (node.isFrozen ? "FROZEN ❄" : "Free"), bx + 10, by + 63);
        gc.setFill(Color.LIME);
        gc.fillText("Total: " + formatBytes(node.totalBytes), bx + 10, by + 83);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Override public boolean isResizable() { return true; }
    @Override public double prefWidth(double h) { return getWidth(); }
    @Override public double prefHeight(double w) { return getHeight(); }
}
