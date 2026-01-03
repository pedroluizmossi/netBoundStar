package com.pedro.netboundstar.view;

import com.pedro.netboundstar.core.AppConfig;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Floating Settings Window.
 * Allows real-time adjustment of application parameters using Sliders.
 */
public class SettingsWindow {

    /**
     * Displays the settings window.
     *
     * @param owner The owner stage (main window).
     */
    public static void show(Stage owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.initStyle(StageStyle.UTILITY);
        stage.setTitle("⚙ Settings");

        AppConfig config = AppConfig.get();

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1a1a20; -fx-text-fill: white;");

        // 1. Slider: Star Lifespan
        root.getChildren().add(createSliderControl("Star Lifespan (s)",
                1.0, 60.0, config.getStarLifeSeconds(),
                newValue -> config.setStarLifeSeconds(newValue)));

        // 2. Slider: Repulsion
        root.getChildren().add(createSliderControl("Repulsion Force",
                100.0, 20000.0, config.getRepulsionForce(),
                newValue -> config.setRepulsionForce(newValue)));

        // 3. Slider: Attraction
        root.getChildren().add(createSliderControl("Central Gravity",
                0.001, 0.05, config.getAttractionForce(),
                newValue -> config.setAttractionForce(newValue)));

        // 4. Slider: Max Speed
        root.getChildren().add(createSliderControl("Max Speed",
                1.0, 50.0, config.getMaxPhysicsSpeed(),
                newValue -> config.setMaxPhysicsSpeed(newValue)));

        // 5. Checkbox: Cluster by Country
        CheckBox cbCluster = new CheckBox("Group by Country (Clustering)");
        cbCluster.setSelected(config.isClusterByCountry());
        cbCluster.setTextFill(Color.ORANGE);
        cbCluster.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        cbCluster.selectedProperty().addListener((obs, oldVal, newVal) -> config.setClusterByCountry(newVal));
        root.getChildren().add(cbCluster);

        // 6. Checkbox: Debug Mode
        CheckBox cbDebug = new CheckBox("Enable Debug Mode (FPS/Metrics)");
        cbDebug.setSelected(config.isDebugMode());
        cbDebug.setTextFill(Color.LIGHTBLUE);
        cbDebug.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        cbDebug.selectedProperty().addListener((obs, oldVal, newVal) -> config.setDebugMode(newVal));
        root.getChildren().add(cbDebug);

        // Save Button
        Button btnSave = new Button("💾 Save Permanently");
        btnSave.setStyle("-fx-padding: 10px; -fx-font-size: 12px; -fx-background-color: #2a7f2a; -fx-text-fill: white;");
        btnSave.setOnAction(e -> {
            config.save();
            stage.close();
        });

        root.getChildren().add(btnSave);

        Scene scene = new Scene(root, 350, 500);
        stage.setScene(scene);
        stage.show();
    }

    private static VBox createSliderControl(String labelText, double min, double max, double current, SliderListener listener) {
        Label label = new Label(labelText + ": " + String.format("%.3f", current));
        label.setTextFill(Color.LIGHTGREEN);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        Slider slider = new Slider(min, max, current);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.setStyle("-fx-control-inner-background: #333; -fx-text-fill: white;");

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double val = newVal.doubleValue();
            label.setText(labelText + ": " + String.format("%.3f", val));
            listener.onChange(val);
        });

        return new VBox(5, label, slider);
    }

    @FunctionalInterface
    interface SliderListener {
        void onChange(double value);
    }
}
