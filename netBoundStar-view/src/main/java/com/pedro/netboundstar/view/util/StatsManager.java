package com.pedro.netboundstar.view.util;

import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.core.model.Protocol;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-time Traffic Statistics Manager.
 * Calculates download/upload speeds, session totals, and history for graphs.
 */
public class StatsManager {

    // Session Totals
    private long totalBytesDown = 0;
    private long totalBytesUp = 0;

    // Current Speed (Bytes per second)
    private long currentDownSpeed = 0;
    private long currentUpSpeed = 0;

    // Temporary accumulators (reset every second)
    private long tempDown = 0;
    private long tempUp = 0;
    private long lastCheckTime = System.currentTimeMillis();

    // Protocol Counts
    private final Map<Protocol, Long> protocolCounts = new ConcurrentHashMap<>();

    // History for the Graph (last 100 points)
    // Each point is an array: [downSpeed, upSpeed]
    private final LinkedList<long[]> history = new LinkedList<>();
    private static final int MAX_HISTORY = 100;

    /**
     * Processes a new captured packet.
     *
     * @param event   The packet event.
     * @param inbound true if it's download, false if it's upload.
     */
    public void process(PacketEvent event, boolean inbound) {
        int size = event.payloadSize();

        // Update totals
        if (inbound) {
            totalBytesDown += size;
            tempDown += size;
        } else {
            totalBytesUp += size;
            tempUp += size;
        }

        // Count Protocol
        protocolCounts.merge(event.protocol(), 1L, Long::sum);
    }

    /**
     * Should be called every frame to update speeds.
     * Calculates speed when 1 second has passed.
     */
    public void tick() {
        long now = System.currentTimeMillis();
        // If 1 second has passed, calculate speed
        if (now - lastCheckTime >= 1000) {
            currentDownSpeed = tempDown;
            currentUpSpeed = tempUp;

            // Add to graph history
            addToHistory(currentDownSpeed, currentUpSpeed);

            // Reset temporary accumulators
            tempDown = 0;
            tempUp = 0;
            lastCheckTime = now;
        }
    }

    /**
     * Adds speed data to the history list.
     *
     * @param down Download speed.
     * @param up   Upload speed.
     */
    private void addToHistory(long down, long up) {
        history.add(new long[]{down, up});
        if (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
    }

    // ========== Getters ==========

    /**
     * Gets total bytes downloaded.
     * @return Total bytes down.
     */
    public long getTotalBytesDown() {
        return totalBytesDown;
    }

    /**
     * Gets total bytes uploaded.
     * @return Total bytes up.
     */
    public long getTotalBytesUp() {
        return totalBytesUp;
    }

    /**
     * Gets current download speed in bytes per second.
     * @return Download speed.
     */
    public long getDownSpeed() {
        return currentDownSpeed;
    }

    /**
     * Gets current upload speed in bytes per second.
     * @return Upload speed.
     */
    public long getUpSpeed() {
        return currentUpSpeed;
    }

    /**
     * Gets the map of protocol counts.
     * @return Protocol counts map.
     */
    public Map<Protocol, Long> getProtocolCounts() {
        return protocolCounts;
    }

    /**
     * Gets the traffic history list.
     * @return History list.
     */
    public LinkedList<long[]> getHistory() {
        return history;
    }

    /**
     * Gets the total number of packets processed.
     * @return Total packets.
     */
    public long getTotalPackets() {
        return protocolCounts.values().stream().mapToLong(Long::longValue).sum();
    }
}
