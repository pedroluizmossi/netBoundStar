package com.pedro.netboundstar.view.util;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Geolocation Service using MaxMind GeoLite2.
 * Resolves IP addresses to countries asynchronously using Virtual Threads (Java 21+).
 */
public class GeoService {

    private static Object reader; 
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private static boolean isAvailable = false;
    
    // Cache to allow synchronous lookup of already resolved IPs
    private static final Map<String, String> ipToCountryCache = new ConcurrentHashMap<>();

    static {
        try {
            Class<?> databaseReaderClass = Class.forName("com.maxmind.geoip2.DatabaseReader");
            Class<?> builderClass = Class.forName("com.maxmind.geoip2.DatabaseReader$Builder");

            InputStream dbStream = GeoService.class.getResourceAsStream("/geo/GeoLite2-Country.mmdb");
            if (dbStream != null) {
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
     * Returns the country code if already in cache, otherwise null.
     */
    public static String getCachedCountry(String ip) {
        return ipToCountryCache.get(ip);
    }

    /**
     * Resolves the ISO country code (e.g., "BR", "US") for an IP address asynchronously.
     */
    public static void resolveCountry(String ip, Consumer<String> callback) {
        if (ip == null) return;
        
        String cached = ipToCountryCache.get(ip);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        if (!isAvailable || reader == null) return;

        if (ip.startsWith("192.168") || ip.startsWith("10.") || ip.equals("127.0.0.1")) {
            return;
        }

        executor.submit(() -> {
            try {
                InetAddress ipAddr = InetAddress.getByName(ip);
                var countryMethod = reader.getClass().getMethod("country", InetAddress.class);
                Object response = countryMethod.invoke(reader, ipAddr);

                var getCountryMethod = response.getClass().getMethod("getCountry");
                Object countryObj = getCountryMethod.invoke(response);

                var getIsoCodeMethod = countryObj.getClass().getMethod("getIsoCode");
                String isoCode = (String) getIsoCodeMethod.invoke(countryObj);

                if (isoCode != null && !isoCode.isEmpty()) {
                    ipToCountryCache.put(ip, isoCode);
                    callback.accept(isoCode);
                }
            } catch (Exception e) {
                // Ignore resolution errors
            }
        });
    }
}
