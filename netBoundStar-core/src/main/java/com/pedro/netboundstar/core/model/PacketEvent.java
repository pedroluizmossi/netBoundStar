package com.pedro.netboundstar.core.model;

import java.time.Instant;

/**
 * Representa um único pacote capturado, simplificado para visualização.
 *
 * @param sourceIp IP de origem (quem enviou)
 * @param targetIp IP de destino (quem recebe)
 * @param protocol O tipo do protocolo (para definir a cor)
 * @param payloadSize O tamanho em bytes (para definir o brilho/tamanho)
 * @param timestamp O momento exato da captura (para sincronia)
 */
public record PacketEvent(
    String sourceIp,
    String targetIp,
    Protocol protocol,
    int payloadSize,
    Instant timestamp
) {
    // Construtor compacto para validações (opcional, mas boa prática)
    public PacketEvent {
        if (payloadSize < 0) payloadSize = 0;
        if (timestamp == null) timestamp = Instant.now();
    }
}

