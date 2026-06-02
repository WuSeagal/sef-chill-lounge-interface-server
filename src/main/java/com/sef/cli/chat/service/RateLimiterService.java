package com.sef.cli.chat.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

@Service
public class RateLimiterService {

    private static final int CAPACITY = 3;
    private static final long WINDOW_MS = 10_000L;
    private static final double REFILL_PER_MS = (double) CAPACITY / WINDOW_MS;

    private final LongSupplier clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterService() {
        this(System::currentTimeMillis);
    }

    RateLimiterService(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * 嘗試為該 userId 消耗一個 token。
     *
     * @return 0 表示放行（已消耗一個 token）；否則回估計還要多少毫秒才會補滿至少一個 token。
     */
    public long tryConsume(String userId) {
        long now = clock.getAsLong();
        Bucket bucket = buckets.computeIfAbsent(userId, k -> new Bucket(CAPACITY, now));
        synchronized (bucket) {
            bucket.refill(now);
            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return 0L;
            }
            double needed = 1.0 - bucket.tokens;
            return (long) Math.ceil(needed / REFILL_PER_MS);
        }
    }

    private static final class Bucket {
        double tokens;
        long lastRefillMs;

        Bucket(double tokens, long lastRefillMs) {
            this.tokens = tokens;
            this.lastRefillMs = lastRefillMs;
        }

        void refill(long now) {
            if (now <= lastRefillMs) return;
            double add = (now - lastRefillMs) * REFILL_PER_MS;
            tokens = Math.min(CAPACITY, tokens + add);
            lastRefillMs = now;
        }
    }
}
