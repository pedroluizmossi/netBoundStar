package com.pedro.netboundstar.core.model;

/**
 * Represents the network protocol of a packet.
 */
public enum Protocol {
    TCP,
    UDP,
    ICMP,
    OTHER;

    /**
     * Utility method to convert a raw string (e.g., "6" or "TCP") into a Protocol enum.
     *
     * @param protocolName The name of the protocol.
     * @return The corresponding Protocol enum, or OTHER if not recognized or null.
     */
    public static Protocol fromString(String protocolName) {
        if (protocolName == null) return OTHER;
        return switch (protocolName.toUpperCase()) {
            case "TCP" -> TCP;
            case "UDP" -> UDP;
            case "ICMP", "ICMPV6" -> ICMP;
            default -> OTHER;
        };
    }
}
