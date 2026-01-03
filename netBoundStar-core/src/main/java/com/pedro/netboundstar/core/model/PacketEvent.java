package com.pedro.netboundstar.core.model;

import java.time.Instant;

/**
 * Represents a single captured packet, simplified for visualization.
 *
 * @param sourceIp The source IP address.
 * @param sourcePort The source port.
 * @param targetIp The destination IP address.
 * @param targetPort The destination port.
 * @param protocol The protocol type (used for color coding).
 * @param payloadSize The size of the payload in bytes (used for brightness/size).
 * @param timestamp The exact moment of capture (used for synchronization).
 */
public record PacketEvent(
    String sourceIp,
    int sourcePort,
    String targetIp,
    int targetPort,
    Protocol protocol,
    int payloadSize,
    Instant timestamp
) {
    /**
     * Compact constructor for validation.
     */
    public PacketEvent {
        if (payloadSize < 0) payloadSize = 0;
        if (timestamp == null) timestamp = Instant.now();
    }
}
