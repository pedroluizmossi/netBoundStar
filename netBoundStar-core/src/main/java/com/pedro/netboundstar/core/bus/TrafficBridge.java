package com.pedro.netboundstar.core.bus;

import com.pedro.netboundstar.core.model.PacketEvent;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TrafficBridge {

    // Instância única (Singleton simples) para facilitar o acesso global neste estágio
    private static final TrafficBridge INSTANCE = new TrafficBridge();

    // Fila concorrente de alta performance
    private final Queue<PacketEvent> eventQueue = new ConcurrentLinkedQueue<>();

    private TrafficBridge() {}

    public static TrafficBridge getInstance() {
        return INSTANCE;
    }

    /**
     * O Sniffer chama isso para publicar um novo pacote.
     */
    public void publish(PacketEvent event) {
        if (event != null) {
            eventQueue.offer(event);
        }
    }

    /**
     * A UI chama isso para pegar o próximo pacote da fila.
     * Retorna null se a fila estiver vazia.
     */
    public PacketEvent poll() {
        return eventQueue.poll();
    }

    /**
     * Utilitário para limpar a fila se ela ficar muito cheia (evitar OutOfMemory)
     * Pode ser chamado se a UI engasgar.
     */
    public void clear() {
        eventQueue.clear();
    }

    public int size() {
        return eventQueue.size();
    }
}

