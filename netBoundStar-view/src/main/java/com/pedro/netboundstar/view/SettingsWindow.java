package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Janela flutuante de Configurações.
 * Permite ajustar em tempo real os parâmetros da aplicação usando Sliders.
 */
public class SettingsWindow {

    /**
     * Exibe a janela de configurações.
     *
     * @param owner Janela proprietária (a janela principal)
     */
    public static void show(Stage owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.NONE); // Permite mexer na janela principal enquanto essa está aberta
        stage.initStyle(StageStyle.UTILITY);
        stage.setTitle("⚙ Ajustes");

        AppConfig config = AppConfig.get();

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1a1a20; -fx-text-fill: white;");

        // 1. Slider: Tempo de Vida
        root.getChildren().add(createSliderControl("Tempo de Vida (s)",
                1.0, 60.0, config.getStarLifeSeconds(),
                newValue -> config.setStarLifeSeconds(newValue)));

        // 2. Slider: Repulsão (Espaço entre estrelas)
        root.getChildren().add(createSliderControl("Força de Repulsão",
                100.0, 20000.0, config.getRepulsionForce(),
                newValue -> config.setRepulsionForce(newValue)));

        // 3. Slider: Atração (Gravidade do centro)
        root.getChildren().add(createSliderControl("Gravidade Central",
                0.001, 0.05, config.getAttractionForce(),
                newValue -> config.setAttractionForce(newValue)));

        // 4. Slider: Velocidade Máxima
        root.getChildren().add(createSliderControl("Velocidade Máxima",
                1.0, 50.0, config.getMaxPhysicsSpeed(),
                newValue -> config.setMaxPhysicsSpeed(newValue)));

        // Botão Salvar
        Button btnSave = new Button("💾 Salvar Definitivamente");
        btnSave.setStyle("-fx-padding: 10px; -fx-font-size: 12px; -fx-background-color: #2a7f2a; -fx-text-fill: white;");
        btnSave.setOnAction(e -> {
            config.save();
            stage.close();
        });

        root.getChildren().add(btnSave);

        Scene scene = new Scene(root, 350, 400);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Cria um controle com Label + Slider.
     * Atualiza a label em tempo real enquanto o slider é movido.
     */
    private static VBox createSliderControl(String labelText, double min, double max, double current, SliderListener listener) {
        Label label = new Label(labelText + ": " + String.format("%.3f", current));
        label.setTextFill(Color.LIGHTGREEN);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        Slider slider = new Slider(min, max, current);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.setStyle("-fx-control-inner-background: #333; -fx-text-fill: white;");

        // Listener em tempo real - atualiza conforme você move o slider
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double val = newVal.doubleValue();
            label.setText(labelText + ": " + String.format("%.3f", val));
            listener.onChange(val);
        });

        return new VBox(5, label, slider);
    }

    /**
     * Interface funcional para callbacks do slider.
     */
    @FunctionalInterface
    interface SliderListener {
        void onChange(double value);
    }
}

