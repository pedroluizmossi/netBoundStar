package com.pedro.netboundstar.view.util;

import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.core.model.Protocol;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de Estatísticas de Tráfego em Tempo Real.
 * Calcula velocidades de download/upload, totais de sessão e histórico para gráficos.
 */
public class StatsManager {

    // Totais da Sessão
    private long totalBytesDown = 0;
    private long totalBytesUp = 0;

    // Velocidade Atual (Bytes por segundo)
    private long currentDownSpeed = 0;
    private long currentUpSpeed = 0;

    // Acumuladores temporários (zeram a cada segundo)
    private long tempDown = 0;
    private long tempUp = 0;
    private long lastCheckTime = System.currentTimeMillis();

    // Contagem de Protocolos
    private final Map<Protocol, Long> protocolCounts = new ConcurrentHashMap<>();

    // Histórico para o Gráfico (últimos 100 pontos)
    // Cada ponto é um array: [downSpeed, upSpeed]
    private final LinkedList<long[]> history = new LinkedList<>();
    private static final int MAX_HISTORY = 100;

    /**
     * Processa um novo pacote capturado.
     * @param event O evento do pacote
     * @param inbound true se é download, false se é upload
     */
    public void process(PacketEvent event, boolean inbound) {
        int size = event.payloadSize();

        // Atualiza totais
        if (inbound) {
            totalBytesDown += size;
            tempDown += size;
        } else {
            totalBytesUp += size;
            tempUp += size;
        }

        // Conta Protocolo
        protocolCounts.merge(event.protocol(), 1L, Long::sum);
    }

    /**
     * Deve ser chamado a cada frame para atualizar as velocidades.
     * Calcula a velocidade quando passa 1 segundo.
     */
    public void tick() {
        long now = System.currentTimeMillis();
        // Se passou 1 segundo, fecha a conta da velocidade
        if (now - lastCheckTime >= 1000) {
            currentDownSpeed = tempDown;
            currentUpSpeed = tempUp;

            // Adiciona ao histórico do gráfico
            addToHistory(currentDownSpeed, currentUpSpeed);

            // Reseta temporários
            tempDown = 0;
            tempUp = 0;
            lastCheckTime = now;
        }
    }

    private void addToHistory(long down, long up) {
        history.add(new long[]{down, up});
        if (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
    }

    // ========== Getters ==========
    public long getTotalBytesDown() {
        return totalBytesDown;
    }

    public long getTotalBytesUp() {
        return totalBytesUp;
    }

    public long getDownSpeed() {
        return currentDownSpeed;
    }

    public long getUpSpeed() {
        return currentUpSpeed;
    }

    public Map<Protocol, Long> getProtocolCounts() {
        return protocolCounts;
    }

    public LinkedList<long[]> getHistory() {
        return history;
    }

    public long getTotalPackets() {
        return protocolCounts.values().stream().mapToLong(Long::longValue).sum();
    }
}

