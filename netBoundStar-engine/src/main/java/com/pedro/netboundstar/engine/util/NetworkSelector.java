package com.pedro.netboundstar.engine.util;

import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.util.List;

/**
 * Utility class for selecting network interfaces.
 */
public class NetworkSelector {

    private static final Logger logger = LoggerFactory.getLogger(NetworkSelector.class);

    private NetworkSelector() {
        // Utility class - do not instantiate
    }

    /**
     * Attempts to automatically find the active network interface.
     * Criterion: Looks for the first interface that has an IPv4 address and is not a loopback interface.
     *
     * @return The selected PcapNetworkInterface.
     * @throws PcapNativeException If no network interfaces are found or an error occurs.
     */
    public static PcapNetworkInterface findActiveInterface() throws PcapNativeException {
        List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();

        if (allDevs == null || allDevs.isEmpty()) {
            throw new PcapNativeException("No network interfaces found. Check permissions (root/admin).");
        }

        for (PcapNetworkInterface device : allDevs) {
            // Ignore loopback interfaces
            if (device.isLoopBack()) continue;

            // Check if it has an associated IPv4 address (usually indicates it's active)
            boolean hasIpv4 = device.getAddresses().stream()
                    .anyMatch(addr -> addr.getAddress() instanceof Inet4Address);

            if (hasIpv4) {
                logger.info("Selected interface: {} - {}", device.getName(), device.getDescription());
                return device;
            }
        }

        // If no ideal interface is found, return the first available as fallback
        return allDevs.getFirst();
    }
}
