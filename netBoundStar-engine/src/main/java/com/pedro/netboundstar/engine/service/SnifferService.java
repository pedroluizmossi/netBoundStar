package com.pedro.netboundstar.engine.service;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.core.bus.TrafficBridge;
import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.core.model.Protocol;
import com.pedro.netboundstar.engine.util.NetworkSelector;
import org.pcap4j.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for sniffing network traffic using Pcap4j.
 * Optimized for Zero-Allocation using Raw Byte Parsing and Ring Buffer.
 * 
 * V4: Added IPv6 support and user-configurable interface selection.
 */
public class SnifferService implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SnifferService.class);
    
    // Keep SNAPLEN large enough for IPv6 headers (Ethernet 14 + IPv6 40 + Transport 4 = 58 min)
    // We need headers for logic, but we need the original length for stats.
    private static final int SNAPLEN = 128;
    
    private static final int READ_TIMEOUT = 10;

    // EtherType constants
    private static final int ETHERTYPE_IPV4 = 0x0800;
    private static final int ETHERTYPE_IPV6 = 0x86DD;

    // IPv6 Header Length is fixed at 40 bytes (extension headers not supported yet)
    private static final int IPV6_HEADER_LENGTH = 40;

    private volatile boolean running = true;
    private PcapHandle handle;

    @Override
    public void run() {
        try {
            PcapNetworkInterface nif;

            // Check if user has configured a specific interface
            String configuredInterface = AppConfig.get().getNetworkInterface();
            if (configuredInterface != null && !configuredInterface.isEmpty()) {
                nif = NetworkSelector.findInterfaceByName(configuredInterface);
                if (nif == null) {
                    logger.warn("Configured interface '{}' not found, falling back to auto-detection", configuredInterface);
                    nif = NetworkSelector.findActiveInterface();
                }
            } else {
                nif = NetworkSelector.findActiveInterface();
            }

            handle = nif.openLive(SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);
            logger.info("Starting capture on: {}", nif.getDescription());

            // Capture both IPv4 and IPv6 traffic
            handle.setFilter("ip or ip6", BpfProgram.BpfCompileMode.OPTIMIZE);

            // Manual Loop to access Original Length
            while (running && handle.isOpen()) {
                try {
                    // getNextRawPacket returns the truncated byte array (max 128 bytes)
                    byte[] data = handle.getNextRawPacket();
                    
                    if (data != null) {
                        // Retrieve the TRUE length of the packet on the wire
                        int originalLength = handle.getOriginalLength();
                        processRawPacket(data, originalLength);
                    }
                } catch (NotOpenException e) {
                    break;
                } catch (Exception e) {
                    // Ignore transient errors
                }
            }

        } catch (PcapNativeException e) {
            logger.error("Failed to start capture engine. Do you have Admin/Root permissions?", e);
        } catch (Exception e) {
            logger.error("Error in capture loop", e);
        } finally {
            if (handle != null && handle.isOpen()) {
                handle.close();
            }
        }
    }

    /**
     * Manually parses the raw bytes to extract IPv4/IPv6 and Transport layer info.
     *
     * @param data The truncated packet data (headers).
     * @param originalLength The actual size of the packet on the wire.
     */
    private void processRawPacket(byte[] data, int originalLength) {
        if (data.length < 34) return; // Minimum for Ethernet + IPv4 header

        // Read EtherType (bytes 12-13)
        int etherType = ((data[12] & 0xFF) << 8) | (data[13] & 0xFF);

        if (etherType == ETHERTYPE_IPV4) {
            processIPv4Packet(data, originalLength);
        } else if (etherType == ETHERTYPE_IPV6) {
            processIPv6Packet(data, originalLength);
        }
        // Other EtherTypes are ignored
    }

    /**
     * Process an IPv4 packet.
     */
    private void processIPv4Packet(byte[] data, int originalLength) {
        byte protoByte = data[23];
        Protocol protocol;
        if (protoByte == 6) protocol = Protocol.TCP;
        else if (protoByte == 17) protocol = Protocol.UDP;
        else protocol = Protocol.OTHER;

        String srcIp = ipv4ToString(data, 26);
        String dstIp = ipv4ToString(data, 30);

        int ihl = (data[14] & 0x0F) * 4;
        int transportOffset = 14 + ihl;

        if (data.length < transportOffset + 4) return;

        int srcPort = ((data[transportOffset] & 0xFF) << 8) | (data[transportOffset + 1] & 0xFF);
        int dstPort = ((data[transportOffset + 2] & 0xFF) << 8) | (data[transportOffset + 3] & 0xFF);

        emitEvent(srcIp, srcPort, dstIp, dstPort, protocol, originalLength);
    }

    /**
     * Process an IPv6 packet.
     * IPv6 header is fixed at 40 bytes. Extension headers are not currently handled.
     */
    private void processIPv6Packet(byte[] data, int originalLength) {
        // Minimum length: Ethernet (14) + IPv6 (40) + Transport (4) = 58 bytes
        if (data.length < 58) return;

        // Next Header field at offset 6 in IPv6 header (offset 20 from Ethernet start)
        byte nextHeader = data[20];
        Protocol protocol;
        if (nextHeader == 6) protocol = Protocol.TCP;
        else if (nextHeader == 17) protocol = Protocol.UDP;
        else protocol = Protocol.OTHER;

        // IPv6 addresses: Source at offset 8-23, Destination at offset 24-39 (relative to IPv6 header)
        // Ethernet header is 14 bytes, so:
        // Source IP: bytes 22-37 (14 + 8)
        // Destination IP: bytes 38-53 (14 + 24)
        String srcIp = ipv6ToString(data, 22);
        String dstIp = ipv6ToString(data, 38);

        // Transport header starts at offset 14 (Ethernet) + 40 (IPv6) = 54
        int transportOffset = 14 + IPV6_HEADER_LENGTH;

        if (data.length < transportOffset + 4) return;

        int srcPort = ((data[transportOffset] & 0xFF) << 8) | (data[transportOffset + 1] & 0xFF);
        int dstPort = ((data[transportOffset + 2] & 0xFF) << 8) | (data[transportOffset + 3] & 0xFF);

        emitEvent(srcIp, srcPort, dstIp, dstPort, protocol, originalLength);
    }

    /**
     * Emit a PacketEvent to the TrafficBridge.
     */
    private void emitEvent(String srcIp, int srcPort, String dstIp, int dstPort, Protocol protocol, int length) {
        TrafficBridge bridge = TrafficBridge.getInstance();
        PacketEvent event = bridge.claimForWrite();

        if (event != null) {
            event.set(srcIp, srcPort, dstIp, dstPort, protocol, length);
            bridge.commitWrite();
        }
    }

    private String ipv4ToString(byte[] data, int offset) {
        return (data[offset] & 0xFF) + "." +
               (data[offset + 1] & 0xFF) + "." +
               (data[offset + 2] & 0xFF) + "." +
               (data[offset + 3] & 0xFF);
    }

    /**
     * Convert IPv6 bytes to standard colon-separated format.
     * Uses compression for consecutive zero groups.
     */
    private String ipv6ToString(byte[] data, int offset) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i > 0) sb.append(':');
            int word = ((data[offset + i * 2] & 0xFF) << 8) | (data[offset + i * 2 + 1] & 0xFF);
            sb.append(Integer.toHexString(word));
        }
        return sb.toString();
    }

    public void stop() {
        this.running = false;
        if (handle != null && handle.isOpen()) {
            try {
                handle.breakLoop();
            } catch (NotOpenException e) {
                // Ignore
            }
        }
    }
}
