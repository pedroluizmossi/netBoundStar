package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.core.model.Protocol;
import javafx.scene.paint.Color;

/**
 * Represents a visual particle representing a network packet traveling between the center and a node.
 */
public class PacketParticle {
    /**
     * Progress of the particle from 0.0 (start) to 1.0 (destination).
     */
    public double progress = 0.0;
    /**
     * Color of the particle based on its protocol.
     */
    public final Color color;
    /**
     * Direction of the particle: true for inbound (download), false for outbound (upload).
     */
    public final boolean inbound;
    /**
     * Speed at which the particle travels.
     */
    public final double speed;

    /**
     * Constructs a new PacketParticle.
     *
     * @param protocol The protocol of the packet.
     * @param inbound  The direction of the packet.
     */
    public PacketParticle(Protocol protocol, boolean inbound) {
        this.inbound = inbound;
        this.color = getColorByProtocol(protocol);
        // Speed is randomly generated within the range defined in AppConfig
        this.speed = AppConfig.get().getRandomParticleSpeed();
    }

    /**
     * Updates the particle's progress based on its speed.
     */
    public void update() {
        progress += speed;
    }

    /**
     * Checks if the particle has reached its destination.
     *
     * @return true if progress >= 1.0.
     */
    public boolean isFinished() {
        return progress >= 1.0;
    }

    /**
     * Determines the color of the particle based on the network protocol.
     *
     * @param p The protocol.
     * @return The corresponding JavaFX Color.
     */
    private Color getColorByProtocol(Protocol p) {
        return switch (p) {
            case TCP -> Color.CYAN;        // Reliable, cool
            case UDP -> Color.ORANGE;      // Fast, hot
            case ICMP -> Color.MAGENTA;    // Ping
            default -> Color.GRAY;
        };
    }
}
