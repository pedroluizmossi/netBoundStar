package com.pedro.netboundstar.view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class StarViewApp extends Application {

    @Override
    public void start(Stage stage) {
        // Cria nosso Canvas customizado
        NetworkCanvas canvas = new NetworkCanvas(800, 600);

        // Layout raiz (StackPane centraliza as coisas por padrão)
        StackPane root = new StackPane(canvas);

        // Faz o canvas seguir o tamanho da janela
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        Scene scene = new Scene(root, 1024, 768);

        stage.setTitle("NetBoundStar :: Network Visualizer");
        stage.setScene(scene);
        stage.show();
    }

    // Método estático para facilitar o lançamento pelo módulo 'app'
    public static void launchApp() {
        launch();
    }
}

