package com.pedro.netboundstar.engine.util;

import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
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
     * Returns a list of all available network interfaces with their details.
     * Useful for displaying in a UI for user selection.
     *
     * @return List of NetworkInterfaceInfo objects.
     */
    public static List<NetworkInterfaceInfo> listAllInterfaces() {
        List<NetworkInterfaceInfo> result = new ArrayList<>();
        try {
            List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
            if (allDevs == null) return result;

            for (PcapNetworkInterface device : allDevs) {
                String name = device.getName();
                String description = device.getDescription() != null ? device.getDescription() : "";
                boolean isLoopback = device.isLoopBack();

                // Collect IP addresses
                List<String> addresses = new ArrayList<>();
                for (var addr : device.getAddresses()) {
                    if (addr.getAddress() != null) {
                        addresses.add(addr.getAddress().getHostAddress());
                    }
                }

                boolean hasIpv4 = device.getAddresses().stream()
                        .anyMatch(addr -> addr.getAddress() instanceof Inet4Address);

                result.add(new NetworkInterfaceInfo(name, description, isLoopback, hasIpv4, addresses));
            }
        } catch (PcapNativeException e) {
            logger.error("Failed to list network interfaces", e);
        }
        return result;
    }

    /**
     * Find a network interface by its name.
     *
     * @param name The interface name (e.g., "eth0", "wlan0", "enp0s3").
     * @return The PcapNetworkInterface or null if not found.
     */
    public static PcapNetworkInterface findInterfaceByName(String name) {
        try {
            List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
            if (allDevs == null) return null;

            for (PcapNetworkInterface device : allDevs) {
                if (device.getName().equals(name)) {
                    logger.info("Found configured interface: {} - {}", device.getName(), device.getDescription());
                    return device;
                }
            }
        } catch (PcapNativeException e) {
            logger.error("Failed to find interface by name: {}", name, e);
        }
        return null;
    }

    /**
     * Attempts to automatically find the active network interface.
     * Criterion: Looks for the first interface that has an IPv4 address and is not a loopback interface.
     * Improved: Filters out common virtual interfaces (docker, veth, virbr, etc.)
     *
     * @return The selected PcapNetworkInterface.
     * @throws PcapNativeException If no network interfaces are found or an error occurs.
     */
    public static PcapNetworkInterface findActiveInterface() throws PcapNativeException {
        List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();

        if (allDevs == null || allDevs.isEmpty()) {
            throw new PcapNativeException("No network interfaces found. Check permissions (root/admin).");
        }

        // First pass: Look for ideal interfaces (non-virtual, non-loopback, with IPv4)
        for (PcapNetworkInterface device : allDevs) {
            if (device.isLoopBack()) continue;
            if (isVirtualInterface(device.getName())) continue;

            boolean hasIpv4 = device.getAddresses().stream()
                    .anyMatch(addr -> addr.getAddress() instanceof Inet4Address);

            if (hasIpv4) {
                logger.info("Selected interface: {} - {}", device.getName(), device.getDescription());
                return device;
            }
        }

        // Second pass: Allow virtual interfaces if no physical ones found
        for (PcapNetworkInterface device : allDevs) {
            if (device.isLoopBack()) continue;

            boolean hasIpv4 = device.getAddresses().stream()
                    .anyMatch(addr -> addr.getAddress() instanceof Inet4Address);

            if (hasIpv4) {
                logger.warn("Using virtual interface (no physical found): {} - {}", device.getName(), device.getDescription());
                return device;
            }
        }

        // If no ideal interface is found, return the first available as fallback
        logger.warn("No ideal interface found, using first available: {}", allDevs.getFirst().getName());
        return allDevs.getFirst();
    }

    /**
     * Check if an interface name matches common virtual interface patterns.
     */
    private static boolean isVirtualInterface(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.startsWith("docker") ||
               lower.startsWith("veth") ||
               lower.startsWith("virbr") ||
               lower.startsWith("br-") ||
               lower.startsWith("vmnet") ||
               lower.startsWith("vboxnet") ||
               lower.contains("virtual") ||
               lower.startsWith("tun") ||
               lower.startsWith("tap");
    }

    /**
     * Data class to hold network interface information for UI display.
     */
    public static class NetworkInterfaceInfo {
        private final String name;
        private final String description;
        private final boolean loopback;
        private final boolean hasIpv4;
        private final List<String> addresses;

        public NetworkInterfaceInfo(String name, String description, boolean loopback, boolean hasIpv4, List<String> addresses) {
            this.name = name;
            this.description = description;
            this.loopback = loopback;
            this.hasIpv4 = hasIpv4;
            this.addresses = addresses;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isLoopback() { return loopback; }
        public boolean hasIpv4() { return hasIpv4; }
        public List<String> getAddresses() { return addresses; }

        /**
         * Returns a display-friendly string for UI.
         */
        public String getDisplayName() {
            StringBuilder sb = new StringBuilder(name);
            if (description != null && !description.isEmpty()) {
                sb.append(" (").append(description).append(")");
            }
            if (!addresses.isEmpty()) {
                sb.append(" - ").append(String.join(", ", addresses));
            }
            if (loopback) {
                sb.append(" [Loopback]");
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }
}
