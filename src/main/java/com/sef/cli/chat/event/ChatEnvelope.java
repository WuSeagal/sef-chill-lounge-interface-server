package com.sef.cli.chat.event;

public record ChatEnvelope<T>(ChatEventType type, long timestamp, T data) {
}
