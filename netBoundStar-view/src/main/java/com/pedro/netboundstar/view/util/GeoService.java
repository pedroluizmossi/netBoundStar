package com.pedro.netboundstar.view.util;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Serviço de Geolocalização usando MaxMind GeoLite2.
 * Resolve IPs para países de forma assíncrona usando Virtual Threads (Java 21+).
 *
 * NOTA: Usa lazy loading para evitar ClassDefNotFoundError se MaxMind não estiver disponível.
 */
public class GeoService {

    private static Object reader; // Object para evitar ClassDefNotFoundError na inicialização
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private static boolean isAvailable = false;

    // Inicialização estática com try-catch robusto
    static {
        try {
            // Tenta carregar a classe DatabaseReader dinamicamente
            Class<?> databaseReaderClass = Class.forName("com.maxmind.geoip2.DatabaseReader");
            Class<?> builderClass = Class.forName("com.maxmind.geoip2.DatabaseReader$Builder");

            InputStream dbStream = GeoService.class.getResourceAsStream("/geo/GeoLite2-Country.mmdb");
            if (dbStream != null) {
                // Usa reflection para criar o DatabaseReader (evita direct import)
                var builderConstructor = builderClass.getConstructor(InputStream.class);
                Object builder = builderConstructor.newInstance(dbStream);

                var buildMethod = builderClass.getMethod("build");
                reader = buildMethod.invoke(builder);

                isAvailable = true;
                System.out.println("✓ GeoLite2 carregado com sucesso!");
            } else {
                System.err.println("⚠ Arquivo GeoLite2-Country.mmdb não encontrado em /resources/geo/");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("⚠ MaxMind GeoIP2 não disponível - bandeiras desativadas (coloque em flags/ como SVG)");
        } catch (Exception e) {
            System.err.println("⚠ Erro ao carregar GeoLite2: " + e.getMessage());
        }
    }

    /**
     * Resolve o código ISO do país (ex: "BR", "US") de um IP de forma assíncrona.
     * Se o IP for local ou GeoService não estiver disponível, a callback não é chamada.
     */
    public static void resolveCountry(String ip, Consumer<String> callback) {
        // Se não carregou ou não está disponível, não faz nada
        if (!isAvailable || reader == null) return;

        // Ignora IPs locais
        if (ip == null || ip.startsWith("192.168") || ip.startsWith("10.") || ip.equals("127.0.0.1")) {
            return;
        }

        // Executa em background
        executor.submit(() -> {
            try {
                InetAddress ipAddr = InetAddress.getByName(ip);

                // Usa reflection para chamar o método country()
                var countryMethod = reader.getClass().getMethod("country", InetAddress.class);
                Object response = countryMethod.invoke(reader, ipAddr);

                // Obtém o país
                var getCountryMethod = response.getClass().getMethod("getCountry");
                Object countryObj = getCountryMethod.invoke(response);

                var getIsoCodeMethod = countryObj.getClass().getMethod("getIsoCode");
                String isoCode = (String) getIsoCodeMethod.invoke(countryObj);

                if (isoCode != null && !isoCode.isEmpty()) {
                    callback.accept(isoCode);
                }
            } catch (Exception e) {
                // Erro ao resolver (IP privado, não encontrado, etc)
            }
        });
    }
}

