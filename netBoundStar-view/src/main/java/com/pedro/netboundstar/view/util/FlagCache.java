package com.pedro.netboundstar.view.util;

import javafx.scene.image.Image;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory Flag Cache.
 * Loads PNG flag files from resources.
 */
public class FlagCache {
    private static final Map<String, Image> cache = new ConcurrentHashMap<>();
    private static final Image UNKNOWN_FLAG = null;

    /**
     * Gets the country flag image.
     *
     * @param countryCode ISO 2-letter country code (e.g., "BR", "US").
     * @return The Image or null if not found.
     */
    public static Image get(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) return UNKNOWN_FLAG;

        return cache.computeIfAbsent(countryCode, code -> {
            String lower = code.toLowerCase();

            // Try to load PNG
            Image img = tryLoadImage("/flags/" + lower + ".png");
            if (img != null) {
                System.out.println("✓ Flag loaded: " + lower + ".png");
                return img;
            }

            System.out.println("⚠ Flag not found: " + code);
            return UNKNOWN_FLAG;
        });
    }

    /**
     * Attempts to load a PNG image from the specified path.
     *
     * @param path The resource path.
     * @return The Image or null if loading fails.
     */
    private static Image tryLoadImage(String path) {
        try {
            InputStream is = FlagCache.class.getResourceAsStream(path);
            if (is != null) {
                Image img = new Image(is);
                if (!img.isError() && img.getWidth() > 0) {
                    return img;
                }
            }
        } catch (Exception e) {
            System.err.println("✗ Error loading " + path + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Clears the flag cache.
     */
    public static void clear() {
        cache.clear();
    }

    /**
     * Returns the number of flags in the cache.
     *
     * @return Cache size.
     */
    public static int size() {
        return cache.size();
    }
}
