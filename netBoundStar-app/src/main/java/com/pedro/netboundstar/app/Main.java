package com.pedro.netboundstar.app;

import com.pedro.netboundstar.core.bus.TrafficBridge;
import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.engine.service.SnifferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static volatile boolean running = true;
    private static SnifferService sniffer;
    private static Thread captureThread;
    private static Thread consoleViewThread;

    public static void main(String[] args) {
        logger.info("╔════════════════════════════════════════════════════════════╗");
        logger.info("║           NetBoundStar - Console Mode                      ║");
        logger.info("║        Network Traffic Visualization Engine                ║");
        logger.info("╚════════════════════════════════════════════════════════════╝");

        // Registrar shutdown hook para parada graciosa
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("🛑 Encerrando aplicação...");
            shutdown();
        }));

        try {
            // 1. Iniciar o Motor de Captura (Producer)
            logger.info("▶ Iniciando motor de captura...");
            sniffer = new SnifferService();
            captureThread = new Thread(sniffer, "Capture-Thread");
            captureThread.setDaemon(false);
            captureThread.start();

            // Aguardar um pouco para permitir ao sniffer inicializar
            Thread.sleep(500);

            // 2. Iniciar o Visualizador de Console (Consumer)
            logger.info("▶ Iniciando visualizador de tráfego...");
            startConsoleViewer();

        } catch (InterruptedException e) {
            logger.error("❌ Erro de interrupção", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("❌ Erro fatal durante inicialização", e);
            shutdown();
            System.exit(1);
        }
    }

    /**
     * Inicia a thread que consome eventos da TrafficBridge e exibe no console
     */
    private static void startConsoleViewer() {
        consoleViewThread = new Thread(() -> {
            logger.info("✓ Sistema pronto. Capturando pacotes... (Ctrl+C para parar)");
            logger.info("─".repeat(70));

            TrafficBridge bridge = TrafficBridge.getInstance();
            long packetCount = 0;
            long lastStatsTime = System.currentTimeMillis();

            while (running) {
                // Tenta pegar um pacote da fila
                PacketEvent event = bridge.poll();

                if (event != null) {
                    packetCount++;
                    // Se tiver pacote, imprime formatado
                    // Ex: [TCP] 192.168.0.1 -> 142.250.1.1 | 1500 bytes
                    System.out.printf("[%8d] [%-5s] %15s → %15s | %5d bytes%n",
                            packetCount,
                            event.protocol(),
                            event.sourceIp(),
                            event.targetIp(),
                            event.payloadSize());

                    // Mostrar estatísticas a cada 30 pacotes
                    if (packetCount % 30 == 0) {
                        long elapsed = System.currentTimeMillis() - lastStatsTime;
                        double pps = (30.0 / elapsed) * 1000;
                        logger.info("📊 {} pacotes capturados | {:.1f} pps", packetCount, pps);
                        lastStatsTime = System.currentTimeMillis();
                    }
                } else {
                    // Se a fila estiver vazia, dorme um pouquinho para não fritar a CPU
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            logger.info("─".repeat(70));
            logger.info("✓ Captura finalizada. Total de {} pacotes", packetCount);
        }, "Console-Viewer-Thread");

        consoleViewThread.setDaemon(false);
        consoleViewThread.start();
    }

    /**
     * Encerra a aplicação de forma graciosa
     */
    private static void shutdown() {
        running = false;

        if (sniffer != null) {
            logger.info("Parando motor de captura...");
            sniffer.stop();
        }

        try {
            if (captureThread != null && captureThread.isAlive()) {
                captureThread.join(2000);
            }
            if (consoleViewThread != null && consoleViewThread.isAlive()) {
                consoleViewThread.join(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("✓ Encerramento completo");
    }
}

