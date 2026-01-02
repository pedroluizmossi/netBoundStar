package com.pedro.netboundstar.app;

import com.pedro.netboundstar.engine.service.SnifferService;
import com.pedro.netboundstar.view.StarViewApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("╔════════════════════════════════════════════════════════════╗");
        logger.info("║           NetBoundStar - Iniciando                         ║");
        logger.info("║        Network Traffic Visualization Engine                ║");
        logger.info("╚════════════════════════════════════════════════════════════╝");

        // 1. Inicia o Sniffer em background (Daemon Thread)
        // Usamos Daemon para que ele morra automaticamente quando fecharmos a janela
        logger.info("▶ Iniciando motor de captura...");
        SnifferService sniffer = new SnifferService();
        Thread captureThread = new Thread(sniffer, "Sniffer-Thread");
        captureThread.setDaemon(true);
        captureThread.start();
        logger.info("✓ Sniffer iniciado em background.");

        // 2. Inicia a Interface Gráfica (Bloqueia a thread main até fechar a janela)
        logger.info("▶ Abrindo janela...");
        StarViewApp.launchApp();
    }
}


