package com.pedro.netboundstar.engine.service;

import com.pedro.netboundstar.core.bus.TrafficBridge;
import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.core.model.Protocol;
import com.pedro.netboundstar.engine.util.NetworkSelector;
import org.pcap4j.core.*;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Service responsible for sniffing network traffic using Pcap4j.
 * It runs in a separate thread and publishes captured packets to the TrafficBridge.
 */
public class SnifferService implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SnifferService.class);

    /**
     * Maximum packet size to capture (65536 bytes covers most cases).
     */
    private static final int SNAPLEN = 65536;

    /**
     * Read timeout in milliseconds.
     */
    private static final int READ_TIMEOUT = 10;

    private volatile boolean running = true;

    @Override
    public void run() {
        try {
            // 1. Find the active network interface
            PcapNetworkInterface nif = NetworkSelector.findActiveInterface();

            // 2. Open the handle for live capture
            // Promiscuous mode = true to see all traffic on the segment
            try (PcapHandle handle = nif.openLive(SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, READ_TIMEOUT)) {

                logger.info("Starting capture on: {}", nif.getDescription());

                // 3. Capture loop
                while (running && handle.isOpen()) {
                    try {
                        // Get the next packet
                        Packet packet = handle.getNextPacket();

                        if (packet != null) {
                            processPacket(packet);
                        }
                    } catch (NotOpenException e) {
                        break;
                    } catch (Exception e) {
                        logger.error("Error processing packet: {}", e.getMessage());
                    }
                }
            }

        } catch (PcapNativeException e) {
            logger.error("Failed to start capture engine. Do you have Admin/Root permissions?", e);
        }
    }

    /**
     * Transforms a raw Pcap4j packet into a clean PacketEvent and publishes it.
     * Currently focuses on IPv4 packets.
     *
     * @param packet The raw packet captured from the network.
     */
    private void processPacket(Packet packet) {
        // Only interested in IPv4 packets for now
        if (packet.contains(IpV4Packet.class)) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);

            String srcIp = ipV4Packet.getHeader().getSrcAddr().getHostAddress();
            String dstIp = ipV4Packet.getHeader().getDstAddr().getHostAddress();
            int length = packet.length();

            // Extract Ports and Protocol
            int srcPort = 0;
            int dstPort = 0;
            Protocol protocol = Protocol.OTHER;

            if (packet.contains(TcpPacket.class)) {
                TcpPacket tcp = packet.get(TcpPacket.class);
                srcPort = tcp.getHeader().getSrcPort().valueAsInt();
                dstPort = tcp.getHeader().getDstPort().valueAsInt();
                protocol = Protocol.TCP;
            } else if (packet.contains(UdpPacket.class)) {
                UdpPacket udp = packet.get(UdpPacket.class);
                srcPort = udp.getHeader().getSrcPort().valueAsInt();
                dstPort = udp.getHeader().getDstPort().valueAsInt();
                protocol = Protocol.UDP;
            }

            // Create immutable event with extracted data
            PacketEvent event = new PacketEvent(
                srcIp, srcPort,
                dstIp, dstPort,
                protocol, length,
                Instant.now()
            );

            // Publish to the bridge for UI consumption
            TrafficBridge.getInstance().publish(event);
        }
    }

    /**
     * Stops the capture loop.
     */
    public void stop() {
        this.running = false;
    }
}
