package com.pedro.netboundstar.engine.service;

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
 */
public class SnifferService implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SnifferService.class);
    private static final int SNAPLEN = 65536;
    private static final int READ_TIMEOUT = 10;

    private volatile boolean running = true;
    private PcapHandle handle;

    @Override
    public void run() {
        try {
            PcapNetworkInterface nif = NetworkSelector.findActiveInterface();

            handle = nif.openLive(SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);
            logger.info("Starting capture on: {}", nif.getDescription());

            // Optimization: Set BPF filter to capture only IPv4 packets at kernel level
            handle.setFilter("ip", BpfProgram.BpfCompileMode.OPTIMIZE);

            // Use loop() with RawPacketListener to avoid Pcap4j object creation overhead
            handle.loop(-1, (RawPacketListener) (rawData) -> {
                if (!running) {
                    try {
                        handle.breakLoop();
                    } catch (NotOpenException e) {
                        // Ignore
                    }
                    return;
                }
                processRawPacket(rawData);
            });

        } catch (PcapNativeException e) {
            logger.error("Failed to start capture engine. Do you have Admin/Root permissions?", e);
        } catch (InterruptedException e) {
            logger.info("Capture interrupted.");
        } catch (NotOpenException e) {
            logger.error("Handle not open", e);
        } finally {
            if (handle != null && handle.isOpen()) {
                handle.close();
            }
        }
    }

    /**
     * Manually parses the raw bytes to extract IPv4 and Transport layer info.
     * This avoids creating heavy Pcap4j packet objects.
     * 
     * Assumptions (Standard Ethernet Frame):
     * - Ethernet Header: 14 bytes
     * - IPv4 Header starts at offset 14
     */
    private void processRawPacket(byte[] data) {
        if (data.length < 34) return; // Too short to contain IP+Ports

        // 1. Ethernet Header is 14 bytes. Check if it's IPv4 (EtherType 0x0800)
        // Byte 12 and 13 are EtherType. 0x08 = 8, 0x00 = 0.
        if (data[12] != 0x08 || data[13] != 0x00) {
            return; // Not IPv4 (though BPF should filter this, double check is cheap)
        }

        // 2. IPv4 Header starts at index 14
        // Protocol is at offset 9 inside IP header -> 14 + 9 = 23
        byte protoByte = data[23];
        Protocol protocol;
        if (protoByte == 6) protocol = Protocol.TCP;
        else if (protoByte == 17) protocol = Protocol.UDP;
        else protocol = Protocol.OTHER;

        // 3. Extract IPs
        // Src IP: Offset 12 inside IP header -> 14 + 12 = 26
        // Dst IP: Offset 16 inside IP header -> 14 + 16 = 30
        String srcIp = ipToString(data, 26);
        String dstIp = ipToString(data, 30);

        // 4. Extract Ports (TCP/UDP)
        // IP Header length is usually 20 bytes (IHL=5).
        // IHL is the lower 4 bits of the first byte of IP header (index 14).
        int ihl = (data[14] & 0x0F) * 4;
        int transportOffset = 14 + ihl;

        if (data.length < transportOffset + 4) return;

        // Ports are the first 4 bytes of TCP/UDP header
        int srcPort = ((data[transportOffset] & 0xFF) << 8) | (data[transportOffset + 1] & 0xFF);
        int dstPort = ((data[transportOffset + 2] & 0xFF) << 8) | (data[transportOffset + 3] & 0xFF);

        // 5. Claim and Publish
        TrafficBridge bridge = TrafficBridge.getInstance();
        PacketEvent event = bridge.claimForWrite();

        if (event != null) {
            event.set(srcIp, srcPort, dstIp, dstPort, protocol, data.length);
            bridge.commitWrite();
        }
    }

    /**
     * Optimized IP to String conversion.
     * Avoids InetAddress.getByAddress() overhead.
     */
    private String ipToString(byte[] data, int offset) {
        // Simple StringBuilder is faster than InetAddress for this purpose
        // and generates less garbage if we could reuse a builder, 
        // but String allocation is unavoidable here unless we change the Model to use byte[] IPs.
        // For now, this is much lighter than Pcap4j's full parsing.
        return (data[offset] & 0xFF) + "." +
               (data[offset + 1] & 0xFF) + "." +
               (data[offset + 2] & 0xFF) + "." +
               (data[offset + 3] & 0xFF);
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
