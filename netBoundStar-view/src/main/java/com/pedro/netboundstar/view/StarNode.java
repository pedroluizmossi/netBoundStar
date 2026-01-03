package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.core.model.Protocol;
import com.pedro.netboundstar.view.util.DnsService;
import com.pedro.netboundstar.view.util.FlagCache;
import com.pedro.netboundstar.view.util.GeoService;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Represents a network node (star) in the visualization.
 * Handles its own position, physics velocity, activity level, and particles.
 */
public class StarNode {
    public double x;
    public double y;
    public double vx = 0;
    public double vy = 0;

    public final String ip;
    public volatile String displayName;
    public double activity = 1.0;

    public boolean isHovered = false;
    public boolean isFrozen = false;

    public long totalBytes = 0;
    public String lastPorts = "N/A";

    /**
     * The color of the last protocol received.
     */
    private Color lastProtocolColor = Color.WHITE;

    public Image flagImage = null;
    private final List<PacketParticle> particles = new ArrayList<>();
    private static final Random random = new Random();

    public StarNode(String ip, double centerX, double centerY) {
        this.ip = ip;
        this.displayName = ip;

        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = 200 + random.nextDouble() * 150;
        this.x = centerX + Math.cos(angle) * distance;
        this.y = centerY + Math.sin(angle) * distance;

        this.vx = (random.nextDouble() - 0.5) * 2.0;
        this.vy = (random.nextDouble() - 0.5) * 2.0;

        DnsService.resolve(ip, resolvedName -> {
            this.displayName = resolvedName;
        });

        GeoService.resolveCountry(ip, isoCode -> {
            this.flagImage = FlagCache.get(isoCode);
        });
    }

    public void pulse(PacketEvent event, boolean inbound) {
        this.activity = 1.0;
        PacketParticle p = new PacketParticle(event.protocol(), inbound);
        particles.add(p);
        
        // Update the star's color to match the latest protocol
        this.lastProtocolColor = p.color;

        this.totalBytes += event.payloadSize();
        if (inbound) {
            this.lastPorts = event.sourcePort() + " -> " + event.targetPort();
        } else {
            this.lastPorts = event.targetPort() + " -> " + event.sourcePort();
        }
    }

    public void update() {
        if (activity > 0) {
            activity -= AppConfig.get().getDecayRatePerFrame();
        }

        Iterator<PacketParticle> it = particles.iterator();
        while (it.hasNext()) {
            PacketParticle p = it.next();
            p.update();
            if (p.isFinished()) {
                it.remove();
            }
        }
    }

    public void drawParticles(GraphicsContext gc, double centerX, double centerY) {
        for (PacketParticle p : particles) {
            double startX, startY, endX, endY;

            if (p.inbound) {
                startX = this.x; startY = this.y;
                endX = centerX; endY = centerY;
            } else {
                startX = centerX; startY = centerY;
                endX = this.x; endY = this.y;
            }

            double currentX = startX + (endX - startX) * p.progress;
            double currentY = startY + (endY - startY) * p.progress;

            gc.setFill(p.color);
            gc.fillOval(currentX - 2, currentY - 2, 4, 4);
        }
    }

    /**
     * Returns the color of the last protocol received.
     * @return The protocol color.
     */
    public Color getLastProtocolColor() {
        return lastProtocolColor;
    }

    public void applyPhysics() {
        this.x += this.vx;
        this.y += this.vy;
        this.vx *= 0.90;
        this.vy *= 0.90;
    }

    public boolean contains(double mx, double my) {
        double dx = this.x - mx;
        double dy = this.y - my;
        return (dx * dx + dy * dy) < (12 * 12);
    }

    public boolean isDead() {
        return activity <= 0 && particles.isEmpty();
    }
}
