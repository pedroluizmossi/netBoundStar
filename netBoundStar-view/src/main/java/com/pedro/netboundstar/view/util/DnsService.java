package com.pedro.netboundstar.view.util;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Serviço de Resolução de DNS Assíncrono.
 * Transforma IPs em Hostnames usando Virtual Threads (Java 21+).
 * Implementa cache para evitar múltiplas requisições do mesmo IP.
 */
public class DnsService {

    // Cache: IP -> Hostname (ex: "8.8.8.8" -> "dns.google")
    private static final Map<String, String> cache = new ConcurrentHashMap<>();

    // Executor usando Virtual Threads (Java 21+) - Perfeito para IO (Rede)
    // Virtual Threads são baratos em memória, então podemos ter milhares simultâneas
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Tenta resolver o hostname de um IP.
     * Se já estiver em cache, retorna imediatamente via callback.
     * Se não, roda em background (Virtual Thread) e chama o callback quando terminar.
     *
     * @param ip IP a resolver (ex: "8.8.8.8")
     * @param onResolved Callback chamado com o hostname resolvido
     */
    public static void resolve(String ip, Consumer<String> onResolved) {
        // 1. Verifica cache rápido (acesso thread-safe)
        if (cache.containsKey(ip)) {
            onResolved.accept(cache.get(ip));
            return;
        }

        // 2. Se não tem em cache, lança a tarefa para uma Virtual Thread
        executor.submit(() -> {
            try {
                // Isso demora (bloqueante), mas como é Virtual Thread, não custa caro para a CPU
                // A thread fica suspensa esperando resposta da rede, liberando o scheduler
                InetAddress inetAddr = InetAddress.getByName(ip);
                String hostname = inetAddr.getCanonicalHostName();

                // Se o Java devolver o próprio IP, significa que não achou nome
                // Neste caso, mantemos o IP mesmo
                if (hostname.equals(ip)) {
                    hostname = ip;
                }

                // Armazena em cache para próximas vezes
                cache.put(ip, hostname);

                // Notifica a UI com o resultado (callback será executado na thread que chamou resolve)
                onResolved.accept(hostname);

            } catch (Exception e) {
                // Em caso de erro (timeout, DNS indisponível, etc), guarda o próprio IP
                // para não tentar resolver de novo
                cache.put(ip, ip);
                onResolved.accept(ip);
            }
        });
    }

    /**
     * Limpa o cache (útil para testes ou se você quiser forçar re-resolução)
     */
    public static void clearCache() {
        cache.clear();
    }

    /**
     * Retorna o tamanho do cache (para debug)
     */
    public static int getCacheSize() {
        return cache.size();
    }
}

