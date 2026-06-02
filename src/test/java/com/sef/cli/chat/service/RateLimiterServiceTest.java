package com.sef.cli.chat.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    @Test
    void allowsFirstThreeThenBlocksFourth() {
        AtomicLong now = new AtomicLong(0L);
        RateLimiterService svc = new RateLimiterService(now::get);
        assertThat(svc.tryConsume("u-1")).isZero();
        assertThat(svc.tryConsume("u-1")).isZero();
        assertThat(svc.tryConsume("u-1")).isZero();
        assertThat(svc.tryConsume("u-1")).isPositive();
    }

    @Test
    void recoversAfterFullWindow() {
        AtomicLong now = new AtomicLong(0L);
        RateLimiterService svc = new RateLimiterService(now::get);
        svc.tryConsume("u-1");
        svc.tryConsume("u-1");
        svc.tryConsume("u-1");
        now.addAndGet(10_000L);
        assertThat(svc.tryConsume("u-1")).isZero();
    }

    @Test
    void bucketsAreIsolatedPerUser() {
        AtomicLong now = new AtomicLong(0L);
        RateLimiterService svc = new RateLimiterService(now::get);
        svc.tryConsume("u-1");
        svc.tryConsume("u-1");
        svc.tryConsume("u-1");
        assertThat(svc.tryConsume("u-1")).isPositive();
        assertThat(svc.tryConsume("u-2")).isZero();
    }

    @Test
    void retryAfterShrinksAsTimePasses() {
        AtomicLong now = new AtomicLong(0L);
        RateLimiterService svc = new RateLimiterService(now::get);
        svc.tryConsume("u-1");
        svc.tryConsume("u-1");
        svc.tryConsume("u-1");
        long immediate = svc.tryConsume("u-1");
        now.addAndGet(2_000L);
        long later = svc.tryConsume("u-1");
        // 鎖定補充率 3/10000ms：空桶需 ceil(1/0.0003)=3334ms；
        // 過 2000ms 補 0.6 token 後，needed=0.4 → ceil(0.4/0.0003)=1334ms。
        assertThat(immediate).isBetween(3300L, 3400L);
        assertThat(later).isBetween(1300L, 1400L).isLessThan(immediate);
    }
}
