package com.pedro.netboundstar.core.model;

import java.time.Instant;

/**
 * Mutable PacketEvent designed for Object Pooling.
 * Replaces the immutable record to avoid GC churn.
 */
public class PacketEvent {
    private String sourceIp;
    private int sourcePort;
    private String targetIp;
    private int targetPort;
    private Protocol protocol;
    private int payloadSize;
    private long timestamp; // Using primitive long for performance (System.nanoTime or epoch)

    public PacketEvent() {
        // Pre-allocate empty
    }

    public void set(String sourceIp, int sourcePort, String targetIp, int targetPort, Protocol protocol, int payloadSize) {
        this.sourceIp = sourceIp;
        this.sourcePort = sourcePort;
        this.targetIp = targetIp;
        this.targetPort = targetPort;
        this.protocol = protocol;
        this.payloadSize = payloadSize;
        this.timestamp = System.currentTimeMillis();
    }

    public String sourceIp() { return sourceIp; }
    public int sourcePort() { return sourcePort; }
    public String targetIp() { return targetIp; }
    public int targetPort() { return targetPort; }
    public Protocol protocol() { return protocol; }
    public int payloadSize() { return payloadSize; }
    public long timestamp() { return timestamp; }
}
