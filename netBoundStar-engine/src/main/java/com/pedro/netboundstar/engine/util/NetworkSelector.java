package com.pedro.netboundstar.engine.util;

import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.util.List;

public class NetworkSelector {

    private static final Logger logger = LoggerFactory.getLogger(NetworkSelector.class);

    private NetworkSelector() {
        // Classe utilitária - não instanciar
    }

    /**
     * Tenta encontrar automaticamente a interface de rede ativa.
     * Critério: Procura a primeira interface que tenha um endereço IP v4 e não seja loopback (127.0.0.1).
     */
    public static PcapNetworkInterface findActiveInterface() throws PcapNativeException {
        List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();

        if (allDevs == null || allDevs.isEmpty()) {
            throw new PcapNativeException("Nenhuma interface de rede encontrada. Verifique as permissões (root/admin).");
        }

        for (PcapNetworkInterface device : allDevs) {
            // Ignora interfaces locais (localhost)
            if (device.isLoopBack()) continue;

            // Verifica se tem um endereço IPv4 associado (geralmente indica que está ligada)
            boolean hasIpv4 = device.getAddresses().stream()
                    .anyMatch(addr -> addr.getAddress() instanceof Inet4Address);

            if (hasIpv4) {
                logger.info("Interface selecionada: {} - {}", device.getName(), device.getDescription());
                return device;
            }
        }

        // Se não encontrar a ideal, devolve a primeira disponível como fallback
        return allDevs.getFirst();
    }
}

