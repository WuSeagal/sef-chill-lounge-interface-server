package com.sef.cli.chat.event.response;

public record RateLimitedPayload(long retryAfterMs) {
}
