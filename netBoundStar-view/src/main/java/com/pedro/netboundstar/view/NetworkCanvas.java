package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.core.bus.TrafficBridge;
import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.core.model.Protocol;
import com.pedro.netboundstar.view.physics.PhysicsEngine;
import com.pedro.netboundstar.view.util.GeoService;
import com.pedro.netboundstar.view.util.StatsManager;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Optimized Canvas for rendering the network visualization.
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

    private double smoothedFps = 60.0;
    private double smoothedFrameTimeMs = 16.6;
    private static final double SMOOTHING_FACTOR = 0.05; 

    private static final long NANOS_PER_FRAME = 1_000_000_000 / 60; 
    private static final long JITTER_THRESHOLD = 1_000_000; 
    private long lastProcessedFrameNanos = 0;
    private long timeAccumulator = 0;

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
                if (lastProcessedFrameNanos == 0) {
                    lastProcessedFrameNanos = now;
                    return;
                }

                long elapsedNanos = now - lastProcessedFrameNanos;
                if (elapsedNanos < (NANOS_PER_FRAME - JITTER_THRESHOLD)) return;

                double currentFrameTimeMs = elapsedNanos / 1_000_000.0;
                double currentFps = 1_000_000_000.0 / elapsedNanos;
                smoothedFrameTimeMs += (currentFrameTimeMs - smoothedFrameTimeMs) * SMOOTHING_FACTOR;
                smoothedFps += (currentFps - smoothedFps) * SMOOTHING_FACTOR;

                lastProcessedFrameNanos = now;
                timeAccumulator += elapsedNanos;
                while (timeAccumulator >= NANOS_PER_FRAME) {
                    if (!paused) updateState();
                    timeAccumulator -= NANOS_PER_FRAME;
                }
                render();
            }
        }.start();
    }

    public void setPaused(boolean paused) { this.paused = paused; }
    public boolean isPaused() { return paused; }
    public void clear() { stars.clear(); }
    public StatsManager getStats() { return stats; }
    public Map<String, StarNode> getStars() { return stars; }

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

            String nodeKey = remoteIp;
            if (config.isClusterByCountry()) {
                String country = GeoService.getCachedCountry(remoteIp);
                if (country != null) {
                    nodeKey = "CLUSTER_" + country;
                }
            }

            StarNode node = stars.get(nodeKey);
            if (node == null) {
                node = new StarNode(remoteIp, centerX, centerY);
                stars.put(nodeKey, node);
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

        gc.setLineWidth(1.0);
        for (StarNode star : stars.values()) {
            if (star.activity < 0.05) continue;
            Color lineColor = star.getLastProtocolColor().deriveColor(0, 1, 1, Math.max(0, star.activity * 0.2));
            gc.setStroke(lineColor);
            gc.strokeLine(centerX, centerY, star.x, star.y);
        }

        for (StarNode star : stars.values()) {
            star.drawParticles(gc, centerX, centerY);
        }

        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
        for (StarNode star : stars.values()) {
            double opacity = Math.max(0, star.activity);
            if (opacity < 0.05) continue;

            // Scale size based on unique hosts (better for clusters)
            int uniqueHosts = star.getUniqueHostCount();
            double baseSize = 6;
            double extraSize = Math.min(24, (uniqueHosts - 1) * 2.0);
            double totalSize = baseSize + extraSize;

            if (star.isFrozen) {
                gc.setStroke(Color.CYAN);
                gc.setLineWidth(2);
                gc.strokeOval(star.x - (totalSize+4)/2, star.y - (totalSize+4)/2, totalSize+4, totalSize+4);
            }

            if (star.isHovered) {
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(2);
                gc.strokeOval(star.x - (totalSize+2)/2, star.y - (totalSize+2)/2, totalSize+2, totalSize+2);
            }

            if (star.flagImage != null && star.flagImage.getWidth() > 0) {
                double flagSize = 16 + extraSize;
                double halfSize = flagSize / 2;
                double flagOpacity = Math.max(0.4, star.activity);
                gc.setGlobalAlpha(flagOpacity);
                gc.drawImage(star.flagImage, star.x - halfSize, star.y - halfSize, flagSize, flagSize);
                gc.setStroke(star.getLastProtocolColor().deriveColor(0, 1, 1, flagOpacity * 0.8));
                gc.setLineWidth(1.0);
                gc.strokeRect(star.x - halfSize, star.y - halfSize, flagSize, flagSize);
                gc.setGlobalAlpha(1.0);
            } else {
                gc.setFill(star.getLastProtocolColor().deriveColor(0, 1, 1, opacity));
                gc.fillOval(star.x - totalSize/2, star.y - totalSize/2, totalSize, totalSize);
            }

            if (star.activity > 0.5 || star.isHovered) {
                gc.setFill(Color.rgb(200, 200, 200, opacity));
                String label = star.getSmartLabel();
                if (star.isCluster()) label += " (" + uniqueHosts + ")";
                gc.fillText(label, star.x + totalSize/2 + 5, star.y + 4);
            }
        }

        double currentRadius = 30 + centerHeat;
        gc.setGlobalAlpha(0.3);
        gc.setFill(Color.CYAN);
        gc.fillOval(centerX - currentRadius, centerY - currentRadius, currentRadius * 2, currentRadius * 2);
        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.WHITE);
        gc.fillOval(centerX - 5, centerY - 5, 10, 10);

        if (hoveredNode != null) drawTooltip(hoveredNode);
        if (AppConfig.get().isDebugMode()) renderDebugInfo();
    }

    private void renderDebugInfo() {
        gc.setFill(Color.rgb(20, 25, 40, 0.8));
        gc.fillRect(10, 100, 180, 80);
        gc.setStroke(Color.LIME);
        gc.setLineWidth(1);
        gc.strokeRect(10, 100, 180, 80);

        gc.setFill(Color.LIME);
        gc.setFont(Font.font("Consolas", 11));
        gc.fillText(String.format("FPS: %.1f", smoothedFps), 20, 120);
        gc.fillText(String.format("Frame: %.2f ms", smoothedFrameTimeMs), 20, 135);
        gc.fillText(String.format("Nodes: %d", stars.size()), 20, 150);
        gc.fillText(String.format("Queue: %d", bridge.size()), 20, 165);
    }

    private void drawTooltip(StarNode node) {
        boolean isCluster = node.isCluster();
        
        double bx = node.x + 20;
        double by = node.y - 20;
        double bw = 260;
        double bh = isCluster ? 130 : 110;

        // Background
        gc.setFill(Color.rgb(20, 20, 30, 0.95));
        gc.setStroke(Color.CYAN);
        gc.setLineWidth(1.5);
        gc.fillRect(bx, by, bw, bh);
        gc.strokeRect(bx, by, bw, bh);

        // Header
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        gc.fillText(node.getSmartLabel(), bx + 10, by + 20);

        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
        gc.setFill(Color.LIGHTGRAY);
        
        int yOff = 40;
        int step = 15;

        if (isCluster) {
            gc.fillText("Type:       Region Cluster", bx + 10, by + yOff); yOff += step;
            gc.fillText("Unique IPs: " + node.getUniqueHostCount(), bx + 10, by + yOff); yOff += step;
            
            // Show top protocol
            Protocol topProto = node.protocolStats.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Protocol.OTHER);
            
            gc.fillText("Main Proto: " + topProto, bx + 10, by + yOff); yOff += step;
            gc.fillText("Packets:    " + node.connectionCount, bx + 10, by + yOff); yOff += step;
        } else {
            gc.fillText("IP Address: " + node.ip, bx + 10, by + yOff); yOff += step;
            gc.fillText("Last Port:  " + node.lastPorts, bx + 10, by + yOff); yOff += step;
            
            Protocol topProto = node.protocolStats.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Protocol.OTHER);
            gc.fillText("Protocol:   " + topProto, bx + 10, by + yOff); yOff += step;
        }

        // Common Stats
        gc.setFill(Color.LIME);
        gc.fillText("Data Transferred: " + formatBytes(node.totalBytes), bx + 10, by + yOff + 5);
        
        // Status
        gc.setFill(node.isFrozen ? Color.CYAN : Color.YELLOW);
        gc.fillText(node.isFrozen ? "❄ FROZEN" : "● ACTIVE", bx + bw - 70, by + 20);
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
