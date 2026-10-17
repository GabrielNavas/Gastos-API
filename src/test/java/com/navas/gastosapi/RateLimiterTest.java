package com.navas.gastosapi;

import com.navas.gastosapi.ratelimit.RateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void permiteAteOLimiteEBloqueiaDepois() {
        AtomicLong now = new AtomicLong(0);
        RateLimiter limiter = new RateLimiter(2, Duration.ofMinutes(1), now::get);

        assertTrue(limiter.tryAcquire("a").allowed());
        assertTrue(limiter.tryAcquire("a").allowed());
        RateLimiter.Result blocked = limiter.tryAcquire("a");

        assertFalse(blocked.allowed());
        assertEquals(60, blocked.retryAfterSeconds());
    }

    @Test
    void chavesDiferentesTemContadoresIndependentes() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1), () -> 0L);

        assertTrue(limiter.tryAcquire("a").allowed());
        assertFalse(limiter.tryAcquire("a").allowed());
        assertTrue(limiter.tryAcquire("b").allowed());
    }

    @Test
    void liberaNovamenteAposAJanela() {
        AtomicLong now = new AtomicLong(0);
        RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1), now::get);

        assertTrue(limiter.tryAcquire("a").allowed());
        assertFalse(limiter.tryAcquire("a").allowed());

        now.set(60_000);
        assertTrue(limiter.tryAcquire("a").allowed());
    }

    @Test
    void retryAfterDiminuiComOPassarDoTempo() {
        AtomicLong now = new AtomicLong(0);
        RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1), now::get);

        limiter.tryAcquire("a");
        now.set(45_000);

        assertEquals(15, limiter.tryAcquire("a").retryAfterSeconds());
    }

    @Test
    void rejeitaLimiteInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(0, Duration.ofMinutes(1)));
    }
}
