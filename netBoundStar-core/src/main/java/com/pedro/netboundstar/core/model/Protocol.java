package com.pedro.netboundstar.core.model;

public enum Protocol {
    TCP,
    UDP,
    ICMP,
    OTHER;

    // Método utilitário para converter string bruta (ex: "6") em Enum
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

