package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.core.model.Protocol;
import com.pedro.netboundstar.view.util.DnsService;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class StarNode {
    public double x, y;

    // NOVOS CAMPOS: Velocidade vetorial para física
    public double vx = 0;
    public double vy = 0;

    public final String ip;

    // NOVO: Nome de exibição (começa como IP, vira hostname depois)
    public volatile String displayName;

    public double activity = 1.0;

    // Estado interativo
    public boolean isHovered = false; // Hover detection
    public boolean isFrozen = false;  // Click to freeze/unfreeze

    // Dados para tooltip
    public long totalBytes = 0;
    public String lastPorts = "N/A";

    // Lista de partículas ativas nesta conexão
    private final List<PacketParticle> particles = new ArrayList<>();

    private static final Random random = new Random();

    public StarNode(String ip, double centerX, double centerY) {
        this.ip = ip;
        this.displayName = ip; // Padrão inicial

        // Posiciona a estrela em uma direção aleatória ao redor do centro
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = 200 + random.nextDouble() * 150;
        this.x = centerX + Math.cos(angle) * distance;
        this.y = centerY + Math.sin(angle) * distance;

        // DICA: Começar com uma velocidade aleatória pequena evita que
        // dois nós criados no mesmo lugar fiquem "grudados" (divisão por zero na física)
        this.vx = (random.nextDouble() - 0.5) * 2.0;
        this.vy = (random.nextDouble() - 0.5) * 2.0;

        // DISPARA A RESOLUÇÃO DE DNS ASSIM QUE NASCE
        DnsService.resolve(ip, resolvedName -> {
            this.displayName = resolvedName;
        });
    }

    // Agora recebe o evento inteiro para extrair informações detalhadas
    public void pulse(PacketEvent event, boolean inbound) {
        this.activity = 1.0;
        // Adiciona uma nova partícula visual viajando na linha
        particles.add(new PacketParticle(event.protocol(), inbound));

        // Acumula dados para tooltip
        this.totalBytes += event.payloadSize();
        // Formata a string de portas
        if (inbound) {
            this.lastPorts = event.sourcePort() + " -> " + event.targetPort();
        } else {
            this.lastPorts = event.targetPort() + " -> " + event.sourcePort();
        }
    }

    public void update() {
        // Mudança aqui: decaimento dinâmico baseado em AppConfig
        if (activity > 0) {
            activity -= AppConfig.get().getDecayRatePerFrame();
        }

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

    // NOVO: Método que a Engine de Física vai chamar para aplicar o movimento calculado
    public void applyPhysics() {
        this.x += this.vx;
        this.y += this.vy;

        // Atrito (Friction): Reduz a velocidade gradualmente para o nó não deslizar para sempre
        // 0.90 significa que ele perde 10% da velocidade a cada frame (efeito de "atmosfera")
        this.vx *= 0.90;
        this.vy *= 0.90;
    }

    // NOVO: Método de detecção de colisão (Hit Testing) para hover/click
    public boolean contains(double mx, double my) {
        double dx = this.x - mx;
        double dy = this.y - my;
        // Raio de 12px para facilitar clicar (um pouco maior que o desenho de 6px)
        return (dx * dx + dy * dy) < (12 * 12);
    }

    public boolean isDead() {
        // Só morre se estiver inativa E sem partículas viajando
        return activity <= 0 && particles.isEmpty();
    }
}

