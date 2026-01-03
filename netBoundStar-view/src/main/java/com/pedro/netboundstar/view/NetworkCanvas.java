package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.core.bus.TrafficBridge;
import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.view.physics.PhysicsEngine;
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

    // Variável para controlar o "inchaço" do centro quando há tráfego
    private double centerHeat = 0.0;

    // Motor de Física para movimento dos nós
    private final PhysicsEngine physics = new PhysicsEngine();

    public NetworkCanvas(double width, double height) {
        super(width, height);
        this.gc = this.getGraphicsContext2D();
        this.bridge = TrafficBridge.getInstance();

        // (Opcional) Configura para as estrelas durarem 10 segundos ao invés de 5
        // Descomente a linha abaixo se quiser ajustar:
        // AppConfig.get().setStarLifeSeconds(10.0);

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

        AppConfig config = AppConfig.get();

        PacketEvent event;
        while ((event = bridge.poll()) != null) {
            // Aumenta o "Calor" do centro quando chega pacote (configurável via AppConfig)
            centerHeat = Math.min(centerHeat + config.getCenterHeatIncrement(), config.getCenterHeatMax());

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

        // Efeito elástico do centro: ele tenta voltar para o tamanho original (0)
        // Usa taxa configurável do AppConfig
        centerHeat *= config.getCenterHeatDecay();

        // 1. Aplica as Forças Físicas e Move tudo
        physics.update(stars.values(), centerX, centerY);

        // 2. Atualiza estados lógicos (Decaimento, Partículas, Morte)
        Iterator<Map.Entry<String, StarNode>> it = stars.entrySet().iterator();
        while (it.hasNext()) {
            StarNode star = it.next().getValue();
            star.update(); // Isso cuida da vida (activity) e partículas

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

        // Limpa Tela
        gc.setFill(Color.rgb(10, 10, 15));
        gc.fillRect(0, 0, w, h);

        // 1. Desenha as linhas de conexão (Camada de trás)
        gc.setLineWidth(1.0);
        for (StarNode star : stars.values()) {
            gc.setStroke(Color.rgb(100, 200, 255, Math.max(0, star.activity * 0.2)));
            gc.strokeLine(centerX, centerY, star.x, star.y);
        }

        // 2. Desenha as partículas (Camada do meio - Onde a mágica acontece)
        for (StarNode star : stars.values()) {
            star.drawParticles(gc, centerX, centerY);
        }

        // 3. Desenha as estrelas e TEXTO (IP ou Hostname)
        gc.setFont(new Font("Consolas", 12)); // Fonte Monospaced fica mais "Hacker"
        for (StarNode star : stars.values()) {
            double opacity = Math.max(0, star.activity);

            // Desenha o ponto da estrela
            gc.setFill(Color.rgb(255, 255, 255, opacity));
            gc.fillOval(star.x - 3, star.y - 3, 6, 6);

            // Desenha o IP ou Hostname apenas se a estrela estiver "viva" o suficiente (> 0.2 de atividade)
            // Isso evita poluição visual de conexões morrendo
            if (star.activity > 0.2) {
                gc.setFill(Color.rgb(200, 200, 200, opacity)); // Texto cinza claro
                // MUDANÇA: Usa displayName em vez de ip (que pode ter mudado para hostname)
                gc.fillText(star.displayName, star.x + 10, star.y + 4);
            }
        }

        // 4. O Núcleo Pulsante (Alterado)
        // O raio base é 30, somamos o "centerHeat" atual
        double currentRadius = 30 + centerHeat;

        // Glow externo do núcleo
        gc.setGlobalAlpha(0.3);
        gc.setFill(Color.CYAN);
        gc.fillOval(centerX - currentRadius, centerY - currentRadius, currentRadius * 2, currentRadius * 2);

        // Núcleo sólido interno (cresce menos, só metade do heat)
        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.WHITE);
        double coreRadius = 5 + (centerHeat * 0.2);
        gc.fillOval(centerX - coreRadius, centerY - coreRadius, coreRadius * 2, coreRadius * 2);

        // HUD
        gc.setFill(Color.LIME);
        gc.setFont(new Font("Consolas", 14));
        gc.fillText("Conexões Ativas: " + stars.size(), 20, 30);
    }

    @Override
    public boolean isResizable() { return true; }
    @Override
    public double prefWidth(double h) { return getWidth(); }
    @Override
    public double prefHeight(double w) { return getHeight(); }
}

