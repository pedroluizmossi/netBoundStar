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

/**
 * Service responsible for sniffing network traffic using Pcap4j.
 * Optimized for Zero-Allocation using the TrafficBridge Ring Buffer and Native Loop.
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
            // This reduces the number of non-IP packets passed to Java
            handle.setFilter("ip", BpfProgram.BpfCompileMode.OPTIMIZE);

            // Use loop() instead of getNextPacket() for better performance
            // -1 means infinite loop until breakLoop() is called
            handle.loop(-1, (PacketListener) packet -> {
                if (!running) {
                    try {
                        handle.breakLoop();
                    } catch (NotOpenException e) {
                        // Ignore
                    }
                    return;
                }
                processPacket(packet);
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

    private void processPacket(Packet packet) {
        // Double check, though BPF should handle it
        if (!packet.contains(IpV4Packet.class)) return;

        // 1. Claim a slot from the Ring Buffer
        TrafficBridge bridge = TrafficBridge.getInstance();
        PacketEvent event = bridge.claimForWrite();

        // If buffer is full (backpressure), event is null. We drop the packet.
        if (event == null) {
            return; 
        }

        // 2. Populate the pooled object (Zero Allocation)
        IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
        String srcIp = ipV4Packet.getHeader().getSrcAddr().getHostAddress();
        String dstIp = ipV4Packet.getHeader().getDstAddr().getHostAddress();
        int length = packet.length();

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

        event.set(srcIp, srcPort, dstIp, dstPort, protocol, length);

        // 3. Commit to make it visible to UI
        bridge.commitWrite();
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
