package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.model.Protocol;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class StarNode {
    public double x, y;
    public final String ip;
    public double activity = 1.0;

    // Lista de partículas ativas nesta conexão
    private final List<PacketParticle> particles = new ArrayList<>();

    private static final Random random = new Random();

    public StarNode(String ip, double centerX, double centerY) {
        this.ip = ip;
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = 200 + random.nextDouble() * 150;
        this.x = centerX + Math.cos(angle) * distance;
        this.y = centerY + Math.sin(angle) * distance;
    }

    // Agora recebe o protocolo e a direção para criar a partícula
    public void pulse(Protocol protocol, boolean inbound) {
        this.activity = 1.0;
        // Adiciona uma nova partícula visual viajando na linha
        particles.add(new PacketParticle(protocol, inbound));
    }

    public void update() {
        if (activity > 0) activity -= 0.005;

        // Atualiza todas as partículas e remove as que chegaram ao destino
        Iterator<PacketParticle> it = particles.iterator();
        while (it.hasNext()) {
            PacketParticle p = it.next();
            p.update();
            if (p.isFinished()) {
                it.remove();
            }
        }
    }

    // Método dedicado para desenhar as partículas desta estrela
    public void drawParticles(GraphicsContext gc, double centerX, double centerY) {
        for (PacketParticle p : particles) {
            double startX, startY, endX, endY;

            if (p.inbound) {
                // Download: Estrela -> Centro
                startX = this.x; startY = this.y;
                endX = centerX; endY = centerY;
            } else {
                // Upload: Centro -> Estrela
                startX = centerX; startY = centerY;
                endX = this.x; endY = this.y;
            }

            // Interpolação Linear (Lerp) para achar a posição atual
            double currentX = startX + (endX - startX) * p.progress;
            double currentY = startY + (endY - startY) * p.progress;

            // Desenha a partícula
            gc.setFill(p.color);
            // Tamanho fixo ou variável? Vamos começar com 4px
            gc.fillOval(currentX - 2, currentY - 2, 4, 4);
        }
    }

    public boolean isDead() {
        // Só morre se estiver inativa E sem partículas viajando
        return activity <= 0 && particles.isEmpty();
    }
}

