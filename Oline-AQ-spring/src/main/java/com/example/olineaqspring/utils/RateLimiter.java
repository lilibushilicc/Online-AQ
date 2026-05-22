package com.example.olineaqspring.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter {

    private static final ConcurrentMap<String, TokenBucket> BUCKETS = new ConcurrentHashMap<>();

    public static boolean tryAcquire(String key, int permitsPerSecond, int burst) {
        TokenBucket bucket = BUCKETS.computeIfAbsent(key, k -> new TokenBucket(permitsPerSecond, burst));
        return bucket.tryAcquire();
    }

    public static void cleanUp() {
        BUCKETS.clear();
    }

    private static class TokenBucket {
        private final int permitsPerSecond;
        private final int burst;
        private final AtomicLong lastRefillTime;
        private final AtomicLong tokens;

        TokenBucket(int permitsPerSecond, int burst) {
            this.permitsPerSecond = permitsPerSecond;
            this.burst = burst;
            this.tokens = new AtomicLong(burst);
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
        }

        boolean tryAcquire() {
            refill();
            while (true) {
                long current = tokens.get();
                if (current <= 0) return false;
                if (tokens.compareAndSet(current, current - 1)) return true;
            }
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long last = lastRefillTime.get();
            long elapsed = now - last;
            if (elapsed < 1000) return;
            if (lastRefillTime.compareAndSet(last, now)) {
                long newTokens = Math.min(burst, tokens.get() + (elapsed * permitsPerSecond / 1000));
                tokens.set(newTokens);
            }
        }
    }
}
