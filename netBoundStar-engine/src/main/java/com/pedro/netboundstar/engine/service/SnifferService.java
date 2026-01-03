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
 * 
 * V3: Restored Traffic Accuracy using getNextRawPacket() + getOriginalLength().
 */
public class SnifferService implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SnifferService.class);
    
    // Keep SNAPLEN small to minimize memory allocation/copying overhead.
    // We only need headers for logic, but we need the original length for stats.
    private static final int SNAPLEN = 128;
    
    private static final int READ_TIMEOUT = 10;

    private volatile boolean running = true;
    private PcapHandle handle;

    @Override
    public void run() {
        try {
            PcapNetworkInterface nif = NetworkSelector.findActiveInterface();

            handle = nif.openLive(SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);
            logger.info("Starting capture on: {}", nif.getDescription());

            handle.setFilter("ip", BpfProgram.BpfCompileMode.OPTIMIZE);

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
     * Manually parses the raw bytes to extract IPv4 and Transport layer info.
     * 
     * @param data The truncated packet data (headers).
     * @param originalLength The actual size of the packet on the wire.
     */
    private void processRawPacket(byte[] data, int originalLength) {
        if (data.length < 34) return; 

        if (data[12] != 0x08 || data[13] != 0x00) {
            return; 
        }

        byte protoByte = data[23];
        Protocol protocol;
        if (protoByte == 6) protocol = Protocol.TCP;
        else if (protoByte == 17) protocol = Protocol.UDP;
        else protocol = Protocol.OTHER;

        String srcIp = ipToString(data, 26);
        String dstIp = ipToString(data, 30);

        int ihl = (data[14] & 0x0F) * 4;
        int transportOffset = 14 + ihl;

        if (data.length < transportOffset + 4) return;

        int srcPort = ((data[transportOffset] & 0xFF) << 8) | (data[transportOffset + 1] & 0xFF);
        int dstPort = ((data[transportOffset + 2] & 0xFF) << 8) | (data[transportOffset + 3] & 0xFF);

        TrafficBridge bridge = TrafficBridge.getInstance();
        PacketEvent event = bridge.claimForWrite();

        if (event != null) {
            // Pass the ORIGINAL length for accurate stats
            event.set(srcIp, srcPort, dstIp, dstPort, protocol, originalLength);
            bridge.commitWrite();
        }
    }

    private String ipToString(byte[] data, int offset) {
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
