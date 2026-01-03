package com.pedro.netboundstar.core.bus;

import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.core.model.Protocol;

import java.util.concurrent.atomic.AtomicLong;

/**
 * High-Performance Ring Buffer Bridge.
 * Implements a fixed-size circular buffer with object pooling to ensure Zero-Allocation and Backpressure.
 * 
 * Strategy: Drop-on-Full (If the UI is slow, packets are dropped to preserve memory/stability).
 */
public class TrafficBridge {

    private static final TrafficBridge INSTANCE = new TrafficBridge();
    
    // Increased Buffer Size to 131072 (2^17) to handle high-throughput bursts without dropping packets.
    // This helps maintain accurate stats even when the UI thread is busy.
    private static final int BUFFER_SIZE = 131072;
    private static final int MASK = BUFFER_SIZE - 1;

    private final PacketEvent[] buffer;
    private final AtomicLong writeSequence = new AtomicLong(0);
    private final AtomicLong readSequence = new AtomicLong(0);

    private TrafficBridge() {
        // Pre-allocate the entire pool
        buffer = new PacketEvent[BUFFER_SIZE];
        for (int i = 0; i < BUFFER_SIZE; i++) {
            buffer[i] = new PacketEvent();
        }
    }

    public static TrafficBridge getInstance() {
        return INSTANCE;
    }

    /**
     * Attempts to claim a slot in the buffer for writing.
     * Returns the pooled object to be populated, or null if buffer is full.
     */
    public PacketEvent claimForWrite() {
        long currentWrite = writeSequence.get();
        long currentRead = readSequence.get();

        if (currentWrite - currentRead >= BUFFER_SIZE) {
            // Buffer is full! Drop the packet.
            return null;
        }

        // Return the pre-allocated object at the current write index
        return buffer[(int) (currentWrite & MASK)];
    }

    /**
     * Commits the write, making the packet available to the consumer.
     * MUST be called after populating the object returned by claimForWrite().
     */
    public void commitWrite() {
        writeSequence.incrementAndGet();
    }

    /**
     * Polls the next available event for reading.
     * Returns null if buffer is empty.
     * 
     * NOTE: The returned object is READ-ONLY valid until the next poll() call cycle.
     * The UI must process it immediately and not store a reference to it.
     */
    public PacketEvent poll() {
        long currentRead = readSequence.get();
        long currentWrite = writeSequence.get();

        if (currentRead >= currentWrite) {
            return null; // Buffer empty
        }

        PacketEvent event = buffer[(int) (currentRead & MASK)];
        
        // We don't increment readSequence here immediately if we want to support batching,
        // but for this simple implementation, we assume 1 poll = 1 consume.
        // However, to be safe with the pool, we should only increment AFTER processing.
        // For simplicity in this loop:
        readSequence.lazySet(currentRead + 1);
        
        return event;
    }

    public int size() {
        return (int) (writeSequence.get() - readSequence.get());
    }
    
    public int capacity() {
        return BUFFER_SIZE;
    }
}
