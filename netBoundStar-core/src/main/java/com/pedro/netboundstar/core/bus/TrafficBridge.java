package com.pedro.netboundstar.core.bus;

import com.pedro.netboundstar.core.model.PacketEvent;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A bridge that facilitates communication between the packet sniffer and the UI.
 * It uses a high-performance concurrent queue to store packet events.
 */
public class TrafficBridge {

    /**
     * Singleton instance for global access.
     */
    private static final TrafficBridge INSTANCE = new TrafficBridge();

    /**
     * High-performance concurrent queue for packet events.
     */
    private final Queue<PacketEvent> eventQueue = new ConcurrentLinkedQueue<>();

    private TrafficBridge() {}

    /**
     * Returns the singleton instance of the TrafficBridge.
     *
     * @return The TrafficBridge instance.
     */
    public static TrafficBridge getInstance() {
        return INSTANCE;
    }

    /**
     * Publishes a new packet event to the queue.
     * Called by the sniffer engine.
     *
     * @param event The packet event to publish.
     */
    public void publish(PacketEvent event) {
        if (event != null) {
            eventQueue.offer(event);
        }
    }

    /**
     * Retrieves and removes the next packet event from the queue.
     * Called by the UI to process events.
     *
     * @return The next PacketEvent, or null if the queue is empty.
     */
    public PacketEvent poll() {
        return eventQueue.poll();
    }

    /**
     * Clears the event queue to prevent memory issues if the UI cannot keep up.
     */
    public void clear() {
        eventQueue.clear();
    }

    /**
     * Returns the current number of events in the queue.
     *
     * @return The queue size.
     */
    public int size() {
        return eventQueue.size();
    }
}
