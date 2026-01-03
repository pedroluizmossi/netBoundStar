package com.pedro.netboundstar.view.util;

import javafx.scene.image.Image;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache de Bandeiras em Memória.
 * Suporta tanto SVG quanto PNG.
 * Evita carregar a mesma imagem do disco múltiplas vezes.
 */
public class FlagCache {
    private static final Map<String, Image> cache = new ConcurrentHashMap<>();
    private static final Image UNKNOWN_FLAG = null; // null = desenha bolinha branca

    /**
     * Obtém a imagem da bandeira do país.
     * Tenta múltiplas variações: SVG/PNG, maiúscula/minúscula.
     *
     * @param countryCode Código ISO de 2 letras (ex: "BR", "US")
     * @return Image ou null se não encontrar
     */
    public static Image get(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) return UNKNOWN_FLAG;

        return cache.computeIfAbsent(countryCode, code -> {
            String upper = code.toUpperCase();
            String lower = code.toLowerCase();

            // Tenta várias combinações (SVG tem prioridade, depois PNG)
            Image img = tryLoadImage("/flags/" + upper + ".svg");
            if (img != null) return img;

            img = tryLoadImage("/flags/" + lower + ".svg");
            if (img != null) return img;

            img = tryLoadImage("/flags/" + upper + ".png");
            if (img != null) return img;

            img = tryLoadImage("/flags/" + lower + ".png");
            if (img != null) return img;

            // Nenhum encontrado (silencioso, sem mensagem de erro)
            return UNKNOWN_FLAG;
        });
    }

    /**
     * Tenta carregar uma imagem do caminho especificado.
     */
    private static Image tryLoadImage(String path) {
        try {
            InputStream is = FlagCache.class.getResourceAsStream(path);
            if (is != null) {
                return new Image(is);
            }
        } catch (Exception e) {
            // Não encontrado ou erro ao carregar
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

