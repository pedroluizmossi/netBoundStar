package com.pedro.netboundstar.view.util;

import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Utility to cache text as images to avoid expensive GlyphLayout calculations every frame.
 */
public class TextImageFactory {

    private static final Text helper = new Text();
    private static final SnapshotParameters params = new SnapshotParameters();

    static {
        helper.setFont(Font.font("Consolas", 12));
        helper.setFill(Color.rgb(200, 200, 200)); // The standard text color
        params.setFill(Color.TRANSPARENT);
    }

    /**
     * Renders a string to an Image.
     * Must be called on the JavaFX Application Thread.
     *
     * @param text The string to render.
     * @return An Image containing the rendered text.
     */
    public static Image create(String text) {
        if (text == null || text.isEmpty()) return null;
        helper.setText(text);
        // Snapshotting is much faster than recalculating Bidi/Glyphs every frame
        return helper.snapshot(params, null);
    }
}
