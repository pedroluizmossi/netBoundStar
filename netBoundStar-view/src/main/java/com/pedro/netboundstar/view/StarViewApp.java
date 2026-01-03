package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.view.util.StatsManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.Timer;
import java.util.TimerTask;

public class StarViewApp extends Application {

    private NetworkCanvas canvas;
    private Label lblPps, lblTotalDown, lblTotalUp;
    private Timer uiUpdateTimer;

    @Override
    public void start(Stage stage) {
        // 1. Camada de Fundo (Canvas)
        canvas = new NetworkCanvas(800, 600);

        // 2. Camada de Interface (HUD)
        // Usamos BorderPane para garantir que o Topo fique no Topo e o Rodapé embaixo.
        BorderPane uiLayer = new BorderPane();
        uiLayer.setPadding(new Insets(20));

        // [CRÍTICO] Permite clicar no Canvas através das áreas vazias da UI
        uiLayer.setPickOnBounds(false);

        // --- BARRA SUPERIOR (HUD + Config) ---
        HBox topBar = createTopBar(stage);
        uiLayer.setTop(topBar);

        // --- BARRA INFERIOR (Legenda + Controles) ---
        HBox bottomBar = createBottomBar();
        uiLayer.setBottom(bottomBar);

        // 3. Raiz (Empilhamento)
        StackPane root = new StackPane(canvas, uiLayer);

        // O Canvas deve redimensionar junto com a janela
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        Scene scene = new Scene(root, 1024, 768);

        // Tenta carregar o CSS (se existir)
        try {
            String css = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Aviso: styles.css não encontrado. A interface ficará sem estilo.");
        }

        stage.setTitle("NetBoundStar :: Network Visualizer");
        stage.setScene(scene);

        // Hooks de fechamento
        stage.setOnCloseRequest(e -> {
            AppConfig.get().save();
            if (uiUpdateTimer != null) uiUpdateTimer.cancel();
            System.exit(0);
        });

        startUiUpdater();
        stage.show();
    }

    private HBox createTopBar(Stage stage) {
        // 1. Painel de Stats (Esquerda)
        HBox statsPanel = createStatsPanel();

        // 2. Botão de Config (Direita)
        Button settingsBtn = new Button("⚙ CONFIG");
        settingsBtn.getStyleClass().addAll("icon-button", "action-button");
        settingsBtn.setOnAction(e -> SettingsWindow.show(stage));

        // 3. Espaçador (Mola)
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Ocupa todo o espaço vazio
        spacer.setPickOnBounds(false); // Permite clicar através do espaço vazio

        // Montagem
        HBox topBar = new HBox(15, statsPanel, spacer, settingsBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPickOnBounds(false); // [CRÍTICO]
        return topBar;
    }

    private HBox createBottomBar() {
        // 1. Legenda (Esquerda)
        HBox legend = createLegend();

        // 2. Controles (Centro/Direita)
        Button btnPause = new Button("⏸");
        btnPause.getStyleClass().add("icon-button");
        btnPause.setTooltip(new Tooltip("Pausar/Retomar"));
        btnPause.setOnAction(e -> {
            boolean isPaused = !canvas.isPaused();
            canvas.setPaused(isPaused);
            btnPause.setText(isPaused ? "▶" : "⏸");
            // Muda a cor do texto inline se o CSS falhar, mas idealmente usa CSS
            btnPause.setStyle(isPaused ? "-fx-text-fill: #ffaa00;" : "");
        });

        Button btnClear = new Button("🗑");
        btnClear.getStyleClass().add("icon-button");
        btnClear.setTooltip(new Tooltip("Limpar Tela"));
        btnClear.setOnAction(e -> canvas.clear());

        HBox controls = new HBox(10, btnPause, btnClear);
        controls.getStyleClass().add("glass-panel");
        controls.setAlignment(Pos.CENTER);

        // 3. Espaçadores para centralizar ou separar
        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        spacerLeft.setPickOnBounds(false);

        // Se quiser os controles no CENTRO absoluto, precisaria de outro spacer na direita.
        // Aqui vamos jogar controles para a Direita junto com o spacer.
        // Layout: [Legenda] <---- VAZIO ----> [Controles]

        HBox bottomBar = new HBox(15, legend, spacerLeft, controls);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPickOnBounds(false); // [CRÍTICO]

        return bottomBar;
    }

    private HBox createStatsPanel() {
        lblPps = new Label("0");
        lblTotalDown = new Label("0 B");
        lblTotalUp = new Label("0 B");

        // Classes CSS
        lblPps.getStyleClass().add("hud-value");
        lblTotalDown.getStyleClass().add("hud-value");
        lblTotalUp.getStyleClass().add("hud-value");

        VBox boxPps = createHudItem("PACOTES", lblPps);
        VBox boxDown = createHudItem("DOWN", lblTotalDown);
        VBox boxUp = createHudItem("UP", lblTotalUp);

        HBox panel = new HBox(20, boxPps, boxDown, boxUp);
        panel.getStyleClass().add("glass-panel");
        panel.setAlignment(Pos.CENTER_LEFT);
        return panel;
    }

    private VBox createHudItem(String title, Label valueLabel) {
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("hud-label");
        return new VBox(2, lblTitle, valueLabel);
    }

    private HBox createLegend() {
        HBox itemTcp = createLegendItem(Color.CYAN, "TCP");
        HBox itemUdp = createLegendItem(Color.ORANGE, "UDP");
        HBox itemOther = createLegendItem(Color.GRAY, "OUTROS");

        HBox legend = new HBox(15, itemTcp, itemUdp, itemOther);
        legend.getStyleClass().add("glass-panel");
        legend.setAlignment(Pos.CENTER_LEFT);
        return legend;
    }

    private HBox createLegendItem(Color color, String text) {
        Circle dot = new Circle(4, color);
        Label lbl = new Label(text);
        lbl.setTextFill(Color.web("#cccccc")); // Fallback cor
        lbl.getStyleClass().add("legend-text");
        return new HBox(5, dot, lbl);
    }

    // --- Atualizador de UI (Thread Segura) ---
    private void startUiUpdater() {
        uiUpdateTimer = new Timer(true);
        uiUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    StatsManager stats = canvas.getStats();
                    if (stats != null) {
                        lblPps.setText(String.valueOf(stats.getTotalPackets()));
                        lblTotalDown.setText(formatBytes(stats.getTotalBytesDown()));
                        lblTotalUp.setText(formatBytes(stats.getTotalBytesUp()));
                    }
                });
            }
        }, 0, 500);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public static void launchApp() {
        launch();
    }
}
