package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import com.pedro.netboundstar.core.health.AppHealth;
import com.pedro.netboundstar.core.health.CapturePrivileges;
import com.pedro.netboundstar.view.util.StatsManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Main JavaFX application class with HUD and Cyberpunk controls.
 */
public class StarViewApp extends Application {

    private NetworkCanvas canvas;
    private Label lblPps, lblTotalDown, lblTotalUp;
    private Timer uiUpdateTimer;

    @Override
    public void start(Stage stage) {
        // 1. Background Layer (Network Canvas)
        canvas = new NetworkCanvas(800, 600);

        // 2. UI Layer (HUD and Controls)
        BorderPane uiLayer = new BorderPane();
        uiLayer.setPadding(new Insets(20));
        uiLayer.setPickOnBounds(false);

        // --- TOP: HUD + Config ---
        VBox topContainer = new VBox(10);
        topContainer.setPickOnBounds(false);
        topContainer.getChildren().addAll(createCaptureWarningBanner(), createTopBar(stage));
        uiLayer.setTop(topContainer);

        // --- BOTTOM: Legend + Controls ---
        HBox bottomBar = createBottomBar();
        uiLayer.setBottom(bottomBar);

        // 3. Root (Stacking)
        StackPane root = new StackPane(canvas, uiLayer);
        
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        Scene scene = new Scene(root, 1024, 768);
        
        try {
            String css = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Warning: styles.css not found.");
        }

        stage.setTitle("NetBoundStar :: Network Visualizer");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            AppConfig.get().save();
            if (uiUpdateTimer != null) uiUpdateTimer.cancel();
            System.exit(0);
        });

        startUiUpdater();
        stage.show();
    }

    private HBox createTopBar(Stage stage) {
        HBox statsPanel = createStatsPanel();
        
        Button settingsBtn = new Button("⚙ CONFIG");
        settingsBtn.getStyleClass().addAll("icon-button", "action-button");
        settingsBtn.setOnAction(e -> SettingsWindow.show(stage));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setPickOnBounds(false);

        HBox topBar = new HBox(15, statsPanel, spacer, settingsBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPickOnBounds(false);
        return topBar;
    }

    private HBox createBottomBar() {
        HBox legend = createLegend();
        
        Button btnPause = new Button("⏸");
        btnPause.getStyleClass().add("icon-button");
        btnPause.setTooltip(new Tooltip("Pause/Resume"));
        btnPause.setOnAction(e -> {
            boolean isPaused = !canvas.isPaused();
            canvas.setPaused(isPaused);
            btnPause.setText(isPaused ? "▶" : "⏸");
            btnPause.setStyle(isPaused ? "-fx-text-fill: #ffaa00;" : "");
        });

        Button btnClear = new Button("🗑");
        btnClear.getStyleClass().add("icon-button");
        btnClear.setTooltip(new Tooltip("Clear Screen"));
        btnClear.setOnAction(e -> canvas.clear());

        HBox controls = new HBox(10, btnPause, btnClear);
        controls.getStyleClass().add("glass-panel");
        controls.setAlignment(Pos.CENTER);

        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        spacerLeft.setPickOnBounds(false);

        HBox bottomBar = new HBox(15, legend, spacerLeft, controls);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPickOnBounds(false);

        return bottomBar;
    }

    private HBox createStatsPanel() {
        lblPps = new Label("0");
        lblTotalDown = new Label("0 B");
        lblTotalUp = new Label("0 B");

        lblPps.getStyleClass().add("hud-value");
        lblTotalDown.getStyleClass().add("hud-value");
        lblTotalUp.getStyleClass().add("hud-value");

        VBox boxPps = createHudItem("PACKETS", lblPps);
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
        HBox itemOther = createLegendItem(Color.GRAY, "OTHER");

        HBox legend = new HBox(15, itemTcp, itemUdp, itemOther);
        legend.getStyleClass().add("glass-panel");
        legend.setAlignment(Pos.CENTER_LEFT);
        return legend;
    }

    private HBox createLegendItem(Color color, String text) {
        Circle dot = new Circle(4, color);
        Label lbl = new Label(text);
        lbl.setTextFill(Color.web("#cccccc"));
        lbl.getStyleClass().add("legend-text");
        return new HBox(5, dot, lbl);
    }

    private Region createCaptureWarningBanner() {
        CapturePrivileges status = AppHealth.getCapturePrivileges();
        String msg = AppHealth.getCapturePrivilegesMessage();

        // Only show banner if privileges are clearly insufficient or status is unknown (best-effort check failed).
        if (status == CapturePrivileges.OK && (msg == null || msg.isBlank())) {
            return new Region();
        }

        String header;
        String details;
        Color color;

        if (status == CapturePrivileges.INSUFFICIENT) {
            header = "Capture permissions missing";
            details = msg != null ? msg : "Run with sudo or grant CAP_NET_RAW to java.";
            color = Color.web("#ff5555");
        } else if (status == CapturePrivileges.OK) {
            header = "Capture permissions: OK";
            details = msg != null ? msg : "";
            color = Color.web("#ffaa00");
        } else {
            header = "Capture permissions unknown";
            details = msg != null ? msg : "Capture may fail unless run as root/Administrator.";
            color = Color.web("#ffaa00");
        }

        Label title = new Label(header);
        title.setTextFill(Color.WHITE);
        title.setStyle("-fx-font-weight: bold;");

        Label body = new Label(details);
        body.setWrapText(true);
        body.setTextFill(Color.web("#eeeeee"));

        Hyperlink hint = new Hyperlink("Show Linux fix (setcap)");
        hint.setStyle("-fx-text-fill: #66ccff;");
        hint.setOnAction(e -> {
            // Print the command to stdout so users can copy. We avoid executing any privileged command.
            System.out.println("Linux fix (note: setcap on /usr/bin/java symlink may fail. Use the real binary):");
            System.out.println("  sudo setcap cap_net_raw,cap_net_admin=eip \"$(readlink -f $(command -v java))\"");
        });

        VBox box = new VBox(4, title, body, hint);
        box.setPadding(new Insets(10));
        box.setStyle(
                "-fx-background-color: rgba(255, 60, 60, 0.18);" +
                "-fx-background-radius: 10;" +
                "-fx-border-radius: 10;" +
                "-fx-border-color: rgba(255,255,255,0.15);" +
                "-fx-border-width: 1;"
        );

        // Slight color cue on the left via border
        box.setBorder(new Border(new BorderStroke(color,
                BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(0, 0, 0, 4))));

        return box;
    }

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
