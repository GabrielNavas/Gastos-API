package com.navas.gastosapi.ratelimit;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Limitador de janela fixa, em memoria, por chave (usuario ou IP).
 * Limitacao conhecida: na virada da janela e possivel um pico de ate 2x o limite.
 * Em memoria significa que o contador nao e compartilhado entre varias instancias;
 * em producao com varias replicas o ideal e um store compartilhado (ex.: Redis).
 */
public class RateLimiter {

    public record Result(boolean allowed, long retryAfterSeconds) {
    }

    private static final int PURGE_THRESHOLD = 10_000;

    private static final class Window {
        private long start;
        private int count;

        private Window(long start) {
            this.start = start;
        }
    }

    private final int limit;
    private final long windowMillis;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int limit, Duration window) {
        this(limit, window, System::currentTimeMillis);
    }

    public RateLimiter(int limit, Duration window, LongSupplier clock) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit deve ser maior que zero");
        }
        this.limit = limit;
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    public Result tryAcquire(String key) {
        long now = clock.getAsLong();
        purgeExpired(now);
        Window window = windows.computeIfAbsent(key, k -> new Window(now));
        synchronized (window) {
            if (now - window.start >= windowMillis) {
                window.start = now;
                window.count = 0;
            }
            if (window.count < limit) {
                window.count++;
                return new Result(true, 0);
            }
            long retryAfterMillis = window.start + windowMillis - now;
            return new Result(false, Math.max(1, (retryAfterMillis + 999) / 1000));
        }
    }

    // Evita crescimento indefinido do mapa (ex.: muitos IPs diferentes)
    private void purgeExpired(long now) {
        if (windows.size() > PURGE_THRESHOLD) {
            windows.entrySet().removeIf(entry -> {
                Window w = entry.getValue();
                synchronized (w) {
                    return now - w.start >= windowMillis;
                }
            });
        }
    }
}
