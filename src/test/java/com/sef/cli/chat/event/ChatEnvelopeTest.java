package com.sef.cli.chat.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sef.cli.chat.event.response.PresenceSnapshotPayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatEnvelopeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesPresenceSnapshotEnvelope() throws Exception {
        ChatEnvelope<PresenceSnapshotPayload> envelope = new ChatEnvelope<>(
                ChatEventType.PRESENCE_SNAPSHOT,
                1700000000000L,
                new PresenceSnapshotPayload(List.of("u-1", "u-2"))
        );

        String json = objectMapper.writeValueAsString(envelope);

        assertThat(json).contains("\"type\":\"PRESENCE_SNAPSHOT\"");
        assertThat(json).contains("\"timestamp\":1700000000000");
        assertThat(json).contains("\"onlineUserIds\":[\"u-1\",\"u-2\"]");
    }

    @Test
    void parsesIncomingEnvelopeAndKeepsRawDataNode() throws Exception {
        String json = "{\"type\":\"PING\",\"timestamp\":1700000000000,\"data\":null}";

        ChatEnvelope<Object> envelope = objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructParametricType(ChatEnvelope.class, Object.class)
        );

        assertThat(envelope.type()).isEqualTo(ChatEventType.PING);
        assertThat(envelope.timestamp()).isEqualTo(1700000000000L);
    }
}
