package com.pedro.netboundstar.view.util;

import javafx.scene.image.Image;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache de Bandeiras em Memória.
 * Carrega arquivos PNG de bandeiras dos recursos.
 */
public class FlagCache {
    private static final Map<String, Image> cache = new ConcurrentHashMap<>();
    private static final Image UNKNOWN_FLAG = null;

    /**
     * Obtém a imagem da bandeira do país.
     * @param countryCode Código ISO de 2 letras (ex: "BR", "US")
     * @return Image ou null se não encontrar
     */
    public static Image get(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) return UNKNOWN_FLAG;

        return cache.computeIfAbsent(countryCode, code -> {
            String lower = code.toLowerCase();

            // Tenta carregar PNG
            Image img = tryLoadImage("/flags/" + lower + ".png");
            if (img != null) {
                System.out.println("✓ Bandeira carregada: " + lower + ".png");
                return img;
            }

            System.out.println("⚠ Bandeira não encontrada: " + code);
            return UNKNOWN_FLAG;
        });
    }

    /**
     * Tenta carregar uma imagem PNG do caminho especificado.
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
            System.err.println("✗ Erro ao carregar " + path + ": " + e.getMessage());
        }
        return null;
    }

    public static void clear() {
        cache.clear();
    }

    public static int size() {
        return cache.size();
    }
}

