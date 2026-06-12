package com.sef.cli.message.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class MessageIdGeneratorTest {

    private final MessageIdGenerator generator = new MessageIdGenerator();

    @Test
    void shouldGenerateTimestampPlusEightCharNanoId() {
        String id = generator.generate();

        assertThat(id).hasSize(20);
        assertThat(id.substring(0, 12)).matches("\\d{12}");
        assertThat(id.substring(12)).matches("[0-9A-Za-z_-]{8}");
    }

    @Test
    void shouldUseTaipeiTimezoneForTimestampPrefix() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-12T16:30:45Z"), ZoneId.of("Asia/Taipei"));
        MessageIdGenerator taipeiGenerator = new MessageIdGenerator(clock);

        String id = taipeiGenerator.generate();

        assertThat(id.substring(0, 12)).isEqualTo("260613003045");
    }
}
