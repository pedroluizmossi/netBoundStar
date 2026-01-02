package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.bus.TrafficBridge;
import com.pedro.netboundstar.core.model.PacketEvent;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NetworkCanvas extends Canvas {

    private final GraphicsContext gc;
    private final TrafficBridge bridge;

    private final Map<String, StarNode> stars = new HashMap<>();

    public NetworkCanvas(double width, double height) {
        super(width, height);
        this.gc = this.getGraphicsContext2D();
        this.bridge = TrafficBridge.getInstance();
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

    // Método auxiliar para detectar direção
    private boolean isInbound(String sourceIp) {
        // Se o IP de origem NÃO for local, é tráfego entrando (Inbound)
        // Se o IP de origem FOR local (192.168...), é tráfego saindo (Outbound)
        return !sourceIp.startsWith("192.168.") && !sourceIp.startsWith("10.") && !sourceIp.equals("127.0.0.1");
    }

    private void updateState() {
        double centerX = getWidth() / 2;
        double centerY = getHeight() / 2;

        PacketEvent event;
        while ((event = bridge.poll()) != null) {
            boolean inbound = isInbound(event.sourceIp());

            // Se for Inbound, o remoto é o Source. Se for Outbound, o remoto é o Target.
            String remoteIp = inbound ? event.sourceIp() : event.targetIp();

            StarNode node = stars.get(remoteIp);
            if (node == null) {
                node = new StarNode(remoteIp, centerX, centerY);
                stars.put(remoteIp, node);
            }

            // Passamos o protocolo e a direção para criar a partícula
            node.pulse(event.protocol(), inbound);
        }

        // Atualiza física
        Iterator<Map.Entry<String, StarNode>> it = stars.entrySet().iterator();
        while (it.hasNext()) {
            StarNode star = it.next().getValue();
            star.update();
            if (star.isDead()) {
                it.remove();
            }
        }
    }

    private void render() {
        double w = getWidth();
        double h = getHeight();
        double centerX = w / 2;
        double centerY = h / 2;

        // Fundo
        gc.setFill(Color.rgb(10, 10, 15));
        gc.fillRect(0, 0, w, h);

        // 1. Desenha as linhas de conexão (Camada de trás)
        gc.setLineWidth(1.0);
        for (StarNode star : stars.values()) {
            gc.setStroke(Color.rgb(100, 200, 255, Math.max(0, star.activity * 0.3))); // Linha sutil
            gc.strokeLine(centerX, centerY, star.x, star.y);
        }

        // 2. Desenha as partículas (Camada do meio - Onde a mágica acontece)
        for (StarNode star : stars.values()) {
            star.drawParticles(gc, centerX, centerY);
        }

        // 3. Desenha os nós das estrelas (Camada da frente)
        for (StarNode star : stars.values()) {
            gc.setFill(Color.rgb(255, 255, 255, Math.max(0, star.activity)));
            gc.fillOval(star.x - 3, star.y - 3, 6, 6);
        }

        // Centro (Localhost)
        gc.setGlobalAlpha(0.2);
        gc.setFill(Color.CYAN);
        gc.fillOval(centerX - 30, centerY - 30, 60, 60);
        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.WHITE);
        gc.fillOval(centerX - 5, centerY - 5, 10, 10);

        // HUD
        gc.setFill(Color.LIME);
        gc.setFont(new Font("Consolas", 14));
        gc.fillText("Conexões: " + stars.size(), 20, 30);
    }

    @Override
    public boolean isResizable() { return true; }
    @Override
    public double prefWidth(double h) { return getWidth(); }
    @Override
    public double prefHeight(double w) { return getHeight(); }
}

