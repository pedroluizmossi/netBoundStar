package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.view.util.DnsService;
import com.pedro.netboundstar.view.util.FlagCache;
import com.pedro.netboundstar.view.util.GeoService;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Represents a network node (star) in the visualization.
 * Handles its own position, physics velocity, activity level, and particles.
 */
public class StarNode {
    /**
     * Current X coordinate.
     */
    public double x;
    /**
     * Current Y coordinate.
     */
    public double y;

    /**
     * X velocity for physics calculations.
     */
    public double vx = 0;
    /**
     * Y velocity for physics calculations.
     */
    public double vy = 0;

    /**
     * The IP address of the node.
     */
    public final String ip;

    /**
     * The display name of the node (IP or resolved hostname).
     */
    public volatile String displayName;

    /**
     * Activity level (1.0 to 0.0). Determines visibility and decay.
     */
    public double activity = 1.0;

    /**
     * Whether the mouse is currently hovering over this node.
     */
    public boolean isHovered = false;
    /**
     * Whether the node is frozen in place by the user.
     */
    public boolean isFrozen = false;

    /**
     * Total bytes transferred by this node.
     */
    public long totalBytes = 0;
    /**
     * Last ports used in communication.
     */
    public String lastPorts = "N/A";

    /**
     * Country flag image for geolocation.
     */
    public Image flagImage = null;

    /**
     * List of active particles traveling to/from this node.
     */
    private final List<PacketParticle> particles = new ArrayList<>();

    private static final Random random = new Random();

    /**
     * Constructs a new StarNode.
     *
     * @param ip      The IP address.
     * @param centerX Initial center X for positioning.
     * @param centerY Initial center Y for positioning.
     */
    public StarNode(String ip, double centerX, double centerY) {
        this.ip = ip;
        this.displayName = ip;

        // Position the star in a random direction around the center
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = 200 + random.nextDouble() * 150;
        this.x = centerX + Math.cos(angle) * distance;
        this.y = centerY + Math.sin(angle) * distance;

        // Start with a small random velocity to avoid overlapping
        this.vx = (random.nextDouble() - 0.5) * 2.0;
        this.vy = (random.nextDouble() - 0.5) * 2.0;

        // Trigger DNS resolution
        DnsService.resolve(ip, resolvedName -> {
            this.displayName = resolvedName;
        });

        // Trigger Geolocation resolution
        GeoService.resolveCountry(ip, isoCode -> {
            this.flagImage = FlagCache.get(isoCode);
        });
    }

    /**
     * Pulses the node when a new packet event occurs.
     *
     * @param event   The packet event.
     * @param inbound true if inbound, false if outbound.
     */
    public void pulse(PacketEvent event, boolean inbound) {
        this.activity = 1.0;
        // Add a new visual particle
        particles.add(new PacketParticle(event.protocol(), inbound));

        // Accumulate data for tooltip
        this.totalBytes += event.payloadSize();
        // Format port string
        if (inbound) {
            this.lastPorts = event.sourcePort() + " -> " + event.targetPort();
        } else {
            this.lastPorts = event.targetPort() + " -> " + event.sourcePort();
        }
    }

    /**
     * Updates the node's state (activity decay and particles).
     */
    public void update() {
        // Dynamic decay based on AppConfig
        if (activity > 0) {
            activity -= AppConfig.get().getDecayRatePerFrame();
        }

        // Update all particles and remove finished ones
        Iterator<PacketParticle> it = particles.iterator();
        while (it.hasNext()) {
            PacketParticle p = it.next();
            p.update();
            if (p.isFinished()) {
                it.remove();
            }
        }
    }

    /**
     * Draws the particles associated with this node.
     *
     * @param gc      The GraphicsContext to draw on.
     * @param centerX The center X of the visualization.
     * @param centerY The center Y of the visualization.
     */
    public void drawParticles(GraphicsContext gc, double centerX, double centerY) {
        for (PacketParticle p : particles) {
            double startX, startY, endX, endY;

            if (p.inbound) {
                // Download: Star -> Center
                startX = this.x; startY = this.y;
                endX = centerX; endY = centerY;
            } else {
                // Upload: Center -> Star
                startX = centerX; startY = centerY;
                endX = this.x; endY = this.y;
            }

            // Linear interpolation (Lerp) for current position
            double currentX = startX + (endX - startX) * p.progress;
            double currentY = startY + (endY - startY) * p.progress;

            // Draw the particle
            gc.setFill(p.color);
            gc.fillOval(currentX - 2, currentY - 2, 4, 4);
        }
    }

    /**
     * Applies physics movement to the node.
     */
    public void applyPhysics() {
        this.x += this.vx;
        this.y += this.vy;

        // Friction: gradually reduce velocity
        this.vx *= 0.90;
        this.vy *= 0.90;
    }

    /**
     * Checks if a point (mouse coordinates) is within the node's hit area.
     *
     * @param mx Mouse X.
     * @param my Mouse Y.
     * @return true if the point is within the node.
     */
    public boolean contains(double mx, double my) {
        double dx = this.x - mx;
        double dy = this.y - my;
        // 12px radius for easier clicking
        return (dx * dx + dy * dy) < (12 * 12);
    }

    /**
     * Determines if the node is "dead" (inactive and no particles).
     *
     * @return true if dead.
     */
    public boolean isDead() {
        return activity <= 0 && particles.isEmpty();
    }
}
