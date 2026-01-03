package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.core.bus.TrafficBridge;
import com.pedro.netboundstar.core.model.PacketEvent;
import com.pedro.netboundstar.view.physics.PhysicsEngine;
import com.pedro.netboundstar.view.util.StatsManager;
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

    // Rastreamento do mouse para hover e click
    private double mouseX = -1000;
    private double mouseY = -1000;
    private StarNode hoveredNode = null;

    // Gerenciador de Estatísticas de Tráfego
    private final StatsManager stats = new StatsManager();

    public NetworkCanvas(double width, double height) {
        super(width, height);
        this.gc = this.getGraphicsContext2D();
        this.bridge = TrafficBridge.getInstance();

        // Listener de MOVIMENTO (Hover)
        this.setOnMouseMoved(e -> {
            this.mouseX = e.getX();
            this.mouseY = e.getY();
        });

        // Listener de CLIQUE (Freeze)
        this.setOnMouseClicked(e -> {
            for (StarNode node : stars.values()) {
                if (node.contains(e.getX(), e.getY())) {
                    node.isFrozen = !node.isFrozen; // Inverte o estado
                    break; // Achou, pode parar
                }
            }
        });

        // (Opcional) Configurar tempo de vida das estrelas se desejar
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

            // Alimenta estatísticas de tráfego
            stats.process(event, inbound);

            // Passamos o evento inteiro para extrair todas as informações
            node.pulse(event, inbound);
        }

        // Atualiza relógio de velocidade (calcula velocidade a cada segundo)
        stats.tick();

        // Efeito elástico do centro: ele tenta voltar para o tamanho original (0)
        // Usa taxa configurável do AppConfig
        centerHeat *= config.getCenterHeatDecay();

        // 1. Aplica as Forças Físicas e Move tudo
        physics.update(stars.values(), centerX, centerY);

        // LÓGICA DE HOVER - Atualiza estado isHovered de todos os nós
        hoveredNode = null;
        for (StarNode node : stars.values()) {
            if (node.contains(mouseX, mouseY)) {
                node.isHovered = true;
                hoveredNode = node; // Guarda para desenhar o tooltip
            } else {
                node.isHovered = false;
            }
        }

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

        // 3. Desenha as estrelas e TEXTO (IP ou Hostname) com indicadores
        gc.setFont(new Font("Consolas", 12)); // Fonte Monospaced fica mais "Hacker"
        for (StarNode star : stars.values()) {
            double opacity = Math.max(0, star.activity);

            // Indicador de CONGELADO (Círculo Ciano)
            if (star.isFrozen) {
                gc.setStroke(Color.CYAN);
                gc.setLineWidth(2);
                gc.strokeOval(star.x - 10, star.y - 10, 20, 20);
            }

            // Indicador de HOVER (Círculo Amarelo)
            if (star.isHovered) {
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(2);
                gc.strokeOval(star.x - 8, star.y - 8, 16, 16);
            }

            // DESENHO DO NÓ - Com suporte a bandeiras (GEO)
            if (star.flagImage != null && star.flagImage.getWidth() > 0) {
                // Se tem bandeira, desenha a imagem com formato circular
                double size = 20; // Tamanho em pixels na tela
                double halfSize = size / 2;
                double flagOpacity = Math.max(0.4, star.activity);

                gc.save(); // Salva o estado atual
                gc.setGlobalAlpha(flagOpacity);

                // Cria clipping circular para fazer a bandeira arredondada
                gc.beginPath();
                gc.arc(star.x, star.y, halfSize, halfSize, 0, 360);
                gc.closePath();
                gc.clip();

                // Desenha a imagem (será cortada pelo clip circular)
                gc.drawImage(star.flagImage, star.x - halfSize, star.y - halfSize, size, size);

                gc.restore(); // Restaura o estado (remove o clip)

                // Opcional: Adiciona borda circular ao redor da bandeira
                gc.setStroke(Color.rgb(255, 255, 255, flagOpacity * 0.5));
                gc.setLineWidth(1);
                gc.strokeOval(star.x - halfSize, star.y - halfSize, size, size);
            } else {
                // Fallback: Desenha a bolinha branca antiga
                gc.setFill(Color.rgb(255, 255, 255, opacity));
                gc.fillOval(star.x - 4, star.y - 4, 8, 8);
            }

            // Desenha o IP ou Hostname apenas se a estrela estiver "viva" o suficiente (> 0.2 de atividade)
            if (star.activity > 0.2) {
                gc.setFill(Color.rgb(200, 200, 200, opacity));
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

        // HUD Superior
        gc.setFill(Color.LIME);
        gc.setFont(new Font("Consolas", 14));
        gc.fillText("Conexões Ativas: " + stars.size() + " | Pacotes: " + stats.getTotalPackets(), 20, 30);

        // Dashboard inferior com estatísticas
        drawDashboard();

        // TOOLTIP (Renderizado por cima de tudo)
        if (hoveredNode != null) {
            drawTooltip(hoveredNode);
        }
    }

    // --- Dashboard de Estatísticas ---
    private void drawDashboard() {
        double w = getWidth();
        double h = getHeight();
        double hudHeight = 100;
        double yStart = h - hudHeight;

        // 1. Fundo do Painel (Vidro escuro)
        gc.setFill(Color.rgb(10, 15, 20, 0.85));
        gc.fillRect(0, yStart, w, hudHeight);

        // Linha de separação neon
        gc.setStroke(Color.rgb(0, 255, 255, 0.5));
        gc.setLineWidth(1);
        gc.strokeLine(0, yStart, w, yStart);

        // 2. Velocímetros (Texto Esquerda)
        gc.setFont(new Font("Consolas", 14));

        // Download (Ciano)
        gc.setFill(Color.CYAN);
        gc.fillText("▼ DOWNLOAD", 20, yStart + 25);
        gc.setFont(new Font("Consolas", 22));
        gc.fillText(formatSpeed(stats.getDownSpeed()), 20, yStart + 50);

        // Upload (Laranja)
        gc.setFont(new Font("Consolas", 14));
        gc.setFill(Color.ORANGE);
        gc.fillText("▲ UPLOAD", 180, yStart + 25);
        gc.setFont(new Font("Consolas", 22));
        gc.fillText(formatSpeed(stats.getUpSpeed()), 180, yStart + 50);

        // Totais (Texto Pequeno abaixo)
        gc.setFont(new Font("Consolas", 11));
        gc.setFill(Color.GRAY);
        gc.fillText("Total: " + formatBytes(stats.getTotalBytesDown()), 20, yStart + 75);
        gc.fillText("Total: " + formatBytes(stats.getTotalBytesUp()), 180, yStart + 75);

        // 3. Gráfico de Histórico (Direita)
        drawGraph(350, yStart + 10, w - 370, hudHeight - 20);
    }

    private void drawGraph(double x, double y, double w, double h) {
        // Fundo do gráfico
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillRect(x, y, w, h);
        gc.setStroke(Color.rgb(50, 50, 50));
        gc.strokeRect(x, y, w, h);

        var history = stats.getHistory();
        if (history.size() < 2) return;

        // Encontrar o pico para escalar o gráfico (Y-Axis automático)
        long maxVal = 1;
        for (long[] point : history) {
            maxVal = Math.max(maxVal, Math.max(point[0], point[1]));
        }

        double stepX = w / (double) (history.size() - 1);

        // Desenha Linha de Download (Ciano)
        gc.setStroke(Color.CYAN);
        gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i < history.size(); i++) {
            double val = history.get(i)[0];
            double px = x + (i * stepX);
            double py = y + h - ((val / (double) maxVal) * h);
            if (i == 0) gc.moveTo(px, py);
            else gc.lineTo(px, py);
        }
        gc.stroke();

        // Desenha Linha de Upload (Laranja)
        gc.setStroke(Color.ORANGE);
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int i = 0; i < history.size(); i++) {
            double val = history.get(i)[1];
            double px = x + (i * stepX);
            double py = y + h - ((val / (double) maxVal) * h);
            if (i == 0) gc.moveTo(px, py);
            else gc.lineTo(px, py);
        }
        gc.stroke();

        // Escala (valor máximo)
        gc.setFill(Color.GRAY);
        gc.setFont(new Font("Consolas", 9));
        gc.fillText(formatBytes(maxVal) + "/s", x + 5, y + 12);
    }

    // Utilitário para formatar velocidade (ex: 1.2 MB/s)
    private String formatSpeed(long bytesPerSec) {
        return formatBytes(bytesPerSec) + "/s";
    }

    // Método auxiliar para formatar bytes em formato legível
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    // Desenha o tooltip com informações detalhadas do nó
    private void drawTooltip(StarNode node) {
        double bx = node.x + 20;
        double by = node.y - 20;
        double bw = 240;
        double bh = 100;

        // Fundo semi-transparente
        gc.setFill(Color.rgb(20, 20, 30, 0.95));
        gc.setStroke(Color.CYAN);
        gc.setLineWidth(1.5);
        gc.fillRect(bx, by, bw, bh);
        gc.strokeRect(bx, by, bw, bh);

        // Texto das informações
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Consolas", 10));
        gc.fillText("Host:  " + node.displayName, bx + 10, by + 18);
        gc.fillText("IP:    " + node.ip, bx + 10, by + 33);
        gc.fillText("Portas: " + node.lastPorts, bx + 10, by + 48);
        gc.fillText("Status: " + (node.isFrozen ? "CONGELADO ❄" : "Livre"), bx + 10, by + 63);

        // Dados com destaque
        gc.setFill(Color.LIME);
        gc.fillText("Total: " + formatBytes(node.totalBytes), bx + 10, by + 83);
    }

    @Override
    public boolean isResizable() { return true; }
    @Override
    public double prefWidth(double h) { return getWidth(); }
    @Override
    public double prefHeight(double w) { return getHeight(); }
}

