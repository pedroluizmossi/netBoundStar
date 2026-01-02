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

public class SnifferService implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SnifferService.class);

    // Tamanho máximo do pacote a capturar (65536 bytes cobre tudo)
    private static final int SNAPLEN = 65536;
    // Tempo limite de leitura em milissegundos
    private static final int READ_TIMEOUT = 10;

    private volatile boolean running = true;

    @Override
    public void run() {
        try {
            // 1. Encontra a interface
            PcapNetworkInterface nif = NetworkSelector.findActiveInterface();

            // 2. Abre o handle (a "torneira" dos dados)
            // Promiscuous mode = true (para ver tudo o que passa)
            try (PcapHandle handle = nif.openLive(SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, READ_TIMEOUT)) {

                logger.info("A iniciar captura em: {}", nif.getDescription());

                // 3. Loop de captura
                while (running && handle.isOpen()) {
                    try {
                        // Pega o próximo pacote
                        Packet packet = handle.getNextPacket();

                        if (packet != null) {
                            processPacket(packet);
                        }
                    } catch (NotOpenException e) {
                        break;
                    } catch (Exception e) {
                        logger.error("Erro ao processar pacote: {}", e.getMessage());
                    }
                }
            }

        } catch (PcapNativeException e) {
            logger.error("Falha ao iniciar o motor de captura. Tem permissões de Admin/Root?", e);
        }
    }

    /**
     * Transforma o pacote bruto da biblioteca Pcap4j no nosso PacketEvent limpo.
     */
    private void processPacket(Packet packet) {
        // Só nos interessam pacotes IPv4 por enquanto (para simplificar)
        if (packet.contains(IpV4Packet.class)) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);

            String srcIp = ipV4Packet.getHeader().getSrcAddr().getHostAddress();
            String dstIp = ipV4Packet.getHeader().getDstAddr().getHostAddress();
            int length = packet.length();

            // Determina o protocolo
            Protocol protocol = Protocol.OTHER;
            if (packet.contains(TcpPacket.class)) {
                protocol = Protocol.TCP;
            } else if (packet.contains(UdpPacket.class)) {
                protocol = Protocol.UDP;
            }

            // Cria o evento imutável
            PacketEvent event = new PacketEvent(
                srcIp,
                dstIp,
                protocol,
                length,
                Instant.now()
            );

            // Publica na ponte para a UI consumir
            TrafficBridge.getInstance().publish(event);
        }
    }

    public void stop() {
        this.running = false;
    }
}

