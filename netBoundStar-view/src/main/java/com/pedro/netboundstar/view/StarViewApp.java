package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class StarViewApp extends Application {

    @Override
    public void start(Stage stage) {
        // Cria nosso Canvas customizado
        NetworkCanvas canvas = new NetworkCanvas(800, 600);

        // Botão de Configurações (⚙)
        Button settingsBtn = new Button("⚙ Config");
        settingsBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); " +
                "-fx-text-fill: #00ff00; " +
                "-fx-cursor: hand; " +
                "-fx-font-size: 11px; " +
                "-fx-padding: 8px 12px;");
        settingsBtn.setOnAction(e -> SettingsWindow.show(stage));

        // StackPane permite empilhar coisas (Canvas embaixo, Botão em cima)
        StackPane root = new StackPane(canvas, settingsBtn);

        // Alinha o botão no topo direito
        StackPane.setAlignment(settingsBtn, Pos.TOP_RIGHT);
        // Margem para não colar na borda
        StackPane.setMargin(settingsBtn, new Insets(10));

        // Faz o canvas seguir o tamanho da janela
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        Scene scene = new Scene(root, 1024, 768);

        stage.setTitle("NetBoundStar :: Network Visualizer");
        stage.setScene(scene);

        // Salva configs ao fechar a janela principal também, por garantia
        stage.setOnCloseRequest(e -> AppConfig.get().save());

        stage.show();
    }

    // Método estático para facilitar o lançamento pelo módulo 'app'
    public static void launchApp() {
        launch();
    }
}



