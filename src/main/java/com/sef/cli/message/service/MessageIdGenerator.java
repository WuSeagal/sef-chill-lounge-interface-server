package com.sef.cli.message.service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class MessageIdGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");

    private final Clock clock;

    public MessageIdGenerator() {
        this(Clock.system(TAIPEI_ZONE));
    }

    MessageIdGenerator(Clock clock) {
        this.clock = clock;
    }

    public String generate() {
        return LocalDateTime.now(clock).format(FORMATTER)
                + NanoIdUtils.randomNanoId(SECURE_RANDOM, NanoIdUtils.DEFAULT_ALPHABET, 8);
    }
}
