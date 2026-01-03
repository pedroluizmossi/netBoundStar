package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Main JavaFX application class for the NetBoundStar visualizer.
 */
public class StarViewApp extends Application {

    @Override
    public void start(Stage stage) {
        // Create our custom Canvas
        NetworkCanvas canvas = new NetworkCanvas(800, 600);

        // Settings Button (⚙)
        Button settingsBtn = new Button("⚙ Config");
        settingsBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); " +
                "-fx-text-fill: #00ff00; " +
                "-fx-cursor: hand; " +
                "-fx-font-size: 11px; " +
                "-fx-padding: 8px 12px;");
        settingsBtn.setOnAction(e -> SettingsWindow.show(stage));

        // StackPane allows stacking elements (Canvas at the bottom, Button on top)
        StackPane root = new StackPane(canvas, settingsBtn);

        // Align the button to the top right
        StackPane.setAlignment(settingsBtn, Pos.TOP_RIGHT);
        // Margin to avoid sticking to the edge
        StackPane.setMargin(settingsBtn, new Insets(10));

        // Make the canvas follow the window size
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        Scene scene = new Scene(root, 1024, 768);

        stage.setTitle("NetBoundStar :: Network Visualizer");
        stage.setScene(scene);

        // Save configurations when closing the main window
        stage.setOnCloseRequest(e -> AppConfig.get().save());

        stage.show();
    }

    /**
     * Static method to launch the application.
     */
    public static void launchApp() {
        launch();
    }
}
