package com.pedro.netboundstar.view.util;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Asynchronous DNS Resolution Service.
 * Resolves IP addresses to hostnames using Virtual Threads (Java 21+).
 * Implements a cache to avoid redundant requests.
 */
public class DnsService {

    /**
     * Cache: IP -> Hostname (e.g., "8.8.8.8" -> "dns.google").
     */
    private static final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Executor using Virtual Threads (Java 21+) - ideal for I/O-bound tasks.
     */
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Attempts to resolve the hostname of an IP address.
     * If cached, returns immediately via callback.
     * Otherwise, runs in the background and calls the callback upon completion.
     *
     * @param ip         The IP address to resolve.
     * @param onResolved Callback invoked with the resolved hostname.
     */
    public static void resolve(String ip, Consumer<String> onResolved) {
        // 1. Check cache first
        if (cache.containsKey(ip)) {
            onResolved.accept(cache.get(ip));
            return;
        }

        // 2. If not in cache, submit task to a Virtual Thread
        executor.submit(() -> {
            try {
                // Blocking network call
                InetAddress inetAddr = InetAddress.getByName(ip);
                String hostname = inetAddr.getCanonicalHostName();

                // If Java returns the IP itself, it means no hostname was found
                if (hostname.equals(ip)) {
                    hostname = ip;
                }

                // Store in cache
                cache.put(ip, hostname);

                // Notify the UI with the result
                onResolved.accept(hostname);

            } catch (Exception e) {
                // On error, cache the IP itself to prevent repeated failed attempts
                cache.put(ip, ip);
                onResolved.accept(ip);
            }
        });
    }

    /**
     * Clears the DNS cache.
     */
    public static void clearCache() {
        cache.clear();
    }

    /**
     * Returns the current size of the DNS cache.
     *
     * @return Cache size.
     */
    public static int getCacheSize() {
        return cache.size();
    }
}
