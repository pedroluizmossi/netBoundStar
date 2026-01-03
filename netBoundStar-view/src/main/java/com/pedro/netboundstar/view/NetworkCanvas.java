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
import javafx.scene.image.Image;
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

    // --- Font Caching ---
    private static final Font FONT_NORMAL = Font.font("Consolas", FontWeight.NORMAL, 12);
    private static final Font FONT_DEBUG = Font.font("Consolas", 11);
    private static final Font FONT_TOOLTIP_HEADER = Font.font("Consolas", FontWeight.BOLD, 14);
    private static final Font FONT_TOOLTIP_BODY = Font.font("Consolas", FontWeight.NORMAL, 11);

    // --- Debug Throttling ---
    private long lastDebugUpdate = 0;
    private String debugFps = "";
    private String debugFrame = "";
    private String debugNodes = "";
    private String debugQueue = "";
    
    // --- Performance Monitoring ---
    private long processedEventsPerFrame = 0;

    // Center "PC" marker size
    private static final double CENTER_DOT_RADIUS = 30; // was 5 (10x10). Now 20x20.

    /**
     * Base radius of the cyan center glow (without heat).
     * Must stay in sync with the collision radius in PhysicsEngine.
     */
    private static final double CENTER_CORE_BASE_RADIUS = 35;

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
                
                // Safety cap: Don't accumulate more than 3 frames to avoid spiral of death
                if (timeAccumulator > NANOS_PER_FRAME * 3) {
                    timeAccumulator = NANOS_PER_FRAME;
                }

                while (timeAccumulator >= NANOS_PER_FRAME) {
                    if (!paused) updateState();
                    timeAccumulator -= NANOS_PER_FRAME;
                }
                render(now);
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
        processedEventsPerFrame = 0;
        
        // Time Budget: 8ms max for event processing to leave time for physics and render
        long startTime = System.nanoTime();
        long maxTime = 8_000_000; // 8ms

        while ((event = bridge.poll()) != null) {
            processedEventsPerFrame++;
            
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
            
            // If we are running out of time, skip visual effects (particles) but keep stats
            boolean skipVisuals = (System.nanoTime() - startTime) > maxTime;
            node.pulse(event, inbound, !skipVisuals);
        }
        
        if (processedEventsPerFrame > 1000) {
            System.out.println("[Lag Warning] Processed " + processedEventsPerFrame + " events in one frame.");
        }

        stats.tick();
        centerHeat *= config.getCenterHeatDecay();
        physics.update(stars.values(), centerX, centerY, CENTER_CORE_BASE_RADIUS + centerHeat);

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

    private void render(long now) {
        double w = getWidth();
        double h = getHeight();
        double centerX = w / 2;
        double centerY = h / 2;

        // Clear screen
        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.rgb(10, 10, 15));
        gc.fillRect(0, 0, w, h);

        // 1. Draw Connections (Lines)
        gc.setLineWidth(1.0);
        for (StarNode star : stars.values()) {
            if (star.activity < 0.05) continue;
            
            // Optimization: Use setGlobalAlpha instead of deriveColor to avoid allocation
            double alpha = Math.max(0, star.activity * 0.2);
            gc.setGlobalAlpha(alpha);
            gc.setStroke(star.getLastProtocolColor());
            gc.strokeLine(centerX, centerY, star.x, star.y);
        }
        gc.setGlobalAlpha(1.0); // Reset alpha

        // 2. Draw Particles
        for (StarNode star : stars.values()) {
            star.drawParticles(gc, centerX, centerY);
        }

        // 3. Draw Nodes (Stars)
        for (StarNode star : stars.values()) {
            double opacity = Math.max(0, star.activity);
            if (opacity < 0.05) continue;

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
                
                // Flag Border
                gc.setGlobalAlpha(flagOpacity * 0.8);
                gc.setStroke(star.getLastProtocolColor());
                gc.setLineWidth(1.0);
                gc.strokeRect(star.x - halfSize, star.y - halfSize, flagSize, flagSize);
            } else {
                // Simple Dot
                gc.setGlobalAlpha(opacity);
                gc.setFill(star.getLastProtocolColor());
                gc.fillOval(star.x - totalSize/2, star.y - totalSize/2, totalSize, totalSize);
            }

            // Labels (Cached Images)
            if (star.activity > 0.5 || star.isHovered) {
                Image labelImg = star.getLabelImage();
                if (labelImg != null) {
                    gc.setGlobalAlpha(opacity);
                    gc.drawImage(labelImg, star.x + totalSize/2 + 5, star.y - 5);
                }
            }
        }
        
        // Reset Alpha for UI elements
        gc.setGlobalAlpha(1.0);

        // Center Core
        double currentRadius = CENTER_CORE_BASE_RADIUS + centerHeat;
        gc.setGlobalAlpha(0.3);
        gc.setFill(Color.CYAN);
        gc.fillOval(centerX - currentRadius, centerY - currentRadius, currentRadius * 2, currentRadius * 2);
        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.WHITE);
        gc.fillOval(centerX - CENTER_DOT_RADIUS, centerY - CENTER_DOT_RADIUS,
                CENTER_DOT_RADIUS * 2, CENTER_DOT_RADIUS * 2);

        if (hoveredNode != null) drawTooltip(hoveredNode);
        if (AppConfig.get().isDebugMode()) renderDebugInfo(now);
    }

    private void renderDebugInfo(long now) {
        // Throttle text updates to 5 times per second (every 200ms)
        if (now - lastDebugUpdate > 200_000_000) {
            debugFps = String.format("FPS: %.1f", smoothedFps);
            debugFrame = String.format("Frame: %.2f ms", smoothedFrameTimeMs);
            debugNodes = String.format("Nodes: %d", stars.size());
            debugQueue = String.format("Queue: %d", bridge.size());
            lastDebugUpdate = now;
        }

        gc.setFill(Color.rgb(20, 25, 40, 0.8));
        gc.fillRect(10, 100, 180, 80);
        gc.setStroke(Color.LIME);
        gc.setLineWidth(1);
        gc.strokeRect(10, 100, 180, 80);

        gc.setFill(Color.LIME);
        gc.setFont(FONT_DEBUG); // Use cached font
        gc.fillText(debugFps, 20, 120);
        gc.fillText(debugFrame, 20, 135);
        gc.fillText(debugNodes, 20, 150);
        gc.fillText(debugQueue, 20, 165);
    }

    private void drawTooltip(StarNode node) {
        boolean isCluster = node.isCluster();
        
        double bx = node.x + 20;
        double by = node.y - 20;
        double bw = 260;
        double bh = isCluster ? 145 : 125; // Increased height for extra stats

        // Background
        gc.setFill(Color.rgb(20, 20, 30, 0.95));
        gc.setStroke(Color.CYAN);
        gc.setLineWidth(1.5);
        gc.fillRect(bx, by, bw, bh);
        gc.strokeRect(bx, by, bw, bh);

        // Header
        gc.setFill(Color.WHITE);
        gc.setFont(FONT_TOOLTIP_HEADER); // Use cached font
        gc.fillText(node.getSmartLabel(), bx + 10, by + 20);

        gc.setFont(FONT_TOOLTIP_BODY); // Use cached font
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
        gc.fillText("Total: " + formatBytes(node.totalBytes), bx + 10, by + yOff + 5);
        
        gc.setFill(Color.CYAN);
        gc.fillText("↓ " + formatBytes(node.bytesDown), bx + 10, by + yOff + 20);
        
        gc.setFill(Color.ORANGE);
        gc.fillText("↑ " + formatBytes(node.bytesUp), bx + 120, by + yOff + 20);
        
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
