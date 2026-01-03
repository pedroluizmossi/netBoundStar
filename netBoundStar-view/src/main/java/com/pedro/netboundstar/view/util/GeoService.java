package com.pedro.netboundstar.view.util;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Geolocation Service using MaxMind GeoLite2.
 * Resolves IP addresses to countries asynchronously using Virtual Threads (Java 21+).
 *
 * NOTE: Uses lazy loading and reflection to avoid ClassDefNotFoundError if MaxMind is not available.
 */
public class GeoService {

    private static Object reader; // Object to avoid ClassDefNotFoundError during initialization
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private static boolean isAvailable = false;

    // Static initialization with robust error handling
    static {
        try {
            // Attempt to load DatabaseReader class dynamically
            Class<?> databaseReaderClass = Class.forName("com.maxmind.geoip2.DatabaseReader");
            Class<?> builderClass = Class.forName("com.maxmind.geoip2.DatabaseReader$Builder");

            InputStream dbStream = GeoService.class.getResourceAsStream("/geo/GeoLite2-Country.mmdb");
            if (dbStream != null) {
                // Use reflection to create the DatabaseReader
                var builderConstructor = builderClass.getConstructor(InputStream.class);
                Object builder = builderConstructor.newInstance(dbStream);

                var buildMethod = builderClass.getMethod("build");
                reader = buildMethod.invoke(builder);

                isAvailable = true;
                System.out.println("✓ GeoLite2 loaded successfully!");
            } else {
                System.err.println("⚠ GeoLite2-Country.mmdb file not found in /resources/geo/");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("⚠ MaxMind GeoIP2 not available - flags disabled");
        } catch (Exception e) {
            System.err.println("⚠ Error loading GeoLite2: " + e.getMessage());
        }
    }

    /**
     * Resolves the ISO country code (e.g., "BR", "US") for an IP address asynchronously.
     *
     * @param ip       The IP address to resolve.
     * @param callback Callback invoked with the resolved ISO country code.
     */
    public static void resolveCountry(String ip, Consumer<String> callback) {
        // If not loaded or available, do nothing
        if (!isAvailable || reader == null) return;

        // Ignore local IPs
        if (ip == null || ip.startsWith("192.168") || ip.startsWith("10.") || ip.equals("127.0.0.1")) {
            return;
        }

        // Execute in background
        executor.submit(() -> {
            try {
                InetAddress ipAddr = InetAddress.getByName(ip);

                // Use reflection to call the country() method
                var countryMethod = reader.getClass().getMethod("country", InetAddress.class);
                Object response = countryMethod.invoke(reader, ipAddr);

                // Get the country object
                var getCountryMethod = response.getClass().getMethod("getCountry");
                Object countryObj = getCountryMethod.invoke(response);

                // Get the ISO code
                var getIsoCodeMethod = countryObj.getClass().getMethod("getIsoCode");
                String isoCode = (String) getIsoCodeMethod.invoke(countryObj);

                if (isoCode != null && !isoCode.isEmpty()) {
                    callback.accept(isoCode);
                }
            } catch (Exception e) {
                // Error resolving (private IP, not found, etc.)
            }
        });
    }
}
