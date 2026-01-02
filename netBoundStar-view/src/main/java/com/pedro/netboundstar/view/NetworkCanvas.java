package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.bus.TrafficBridge;
import com.pedro.netboundstar.core.model.PacketEvent;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class NetworkCanvas extends Canvas {

    private final GraphicsContext gc;
    private final TrafficBridge bridge;

    // Estatísticas simples para debug visual
    private long totalPackets = 0;
    private String lastIp = "Aguardando...";

    public NetworkCanvas(double width, double height) {
        super(width, height);
        this.gc = this.getGraphicsContext2D();
        this.bridge = TrafficBridge.getInstance();

        // Inicia o Loop de Animação
        startLoop();
    }

    private void startLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateState();
                render();
            }
        }.start();
    }

    // Passo 1: Lógica (Consumir dados e calcular física)
    private void updateState() {
        // Processa todos os pacotes que chegaram desde o último frame
        // Isso evita que a fila cresça infinitamente
        PacketEvent event;
        while ((event = bridge.poll()) != null) {
            totalPackets++;
            lastIp = event.sourceIp() + " -> " + event.targetIp();

            // TODO: Aqui vamos criar as "Estrelas" na Fase 3
        }
    }

    // Passo 2: Desenho (Limpar e Pintar)
    private void render() {
        double w = getWidth();
        double h = getHeight();

        // 1. Fundo Cyberpunk (Preto Profundo)
        gc.setFill(Color.rgb(10, 10, 15));
        gc.fillRect(0, 0, w, h);

        // 2. O "Sol" (Seu Computador) no centro
        double centerX = w / 2;
        double centerY = h / 2;

        // Efeito de brilho (Glow simples)
        gc.setGlobalAlpha(0.2);
        gc.setFill(Color.CYAN);
        gc.fillOval(centerX - 30, centerY - 30, 60, 60);

        // Núcleo sólido
        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.WHITE);
        gc.fillOval(centerX - 5, centerY - 5, 10, 10);

        // 3. HUD (Heads-up Display) - Texto informativo
        gc.setFill(Color.LIME);
        gc.setFont(new Font("Consolas", 14));
        gc.fillText("FPS: 60 (Simulado)", 20, 30);
        gc.fillText("Total Pacotes: " + totalPackets, 20, 50);
        gc.fillText("Última Conexão: " + lastIp, 20, 70);
    }

    // Garante que o canvas redimensione junto com a janela
    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }
}

