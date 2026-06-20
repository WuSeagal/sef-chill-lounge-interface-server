package com.sef.cli.chat.service;

import com.sef.cli.chat.event.ChatEnvelope;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.event.response.PresenceSnapshotPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PresenceReaperServiceTest {

    private OnlineUserService onlineUserService;
    private ChatBroadcastService broadcastService;
    private PresenceReaperService reaper;

    @BeforeEach
    void setUp() {
        onlineUserService = mock(OnlineUserService.class);
        broadcastService = mock(ChatBroadcastService.class);
        reaper = new PresenceReaperService(onlineUserService, broadcastService);
    }

    @Test
    void rebroadcastsPresenceSnapshotWhenSessionsReaped() {
        when(onlineUserService.reapStale(90_000L)).thenReturn(List.of("ghost-1", "ghost-2"));
        when(onlineUserService.getOnlineUserIds()).thenReturn(java.util.Set.of("u-1"));

        reaper.reap();

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService).broadcastToAll(captor.capture());
        ChatEnvelope<?> sent = captor.getValue();
        assertThat(sent.type()).isEqualTo(ChatEventType.PRESENCE_SNAPSHOT);
        PresenceSnapshotPayload payload = (PresenceSnapshotPayload) sent.data();
        assertThat(payload.onlineUserIds()).containsExactly("u-1");
    }

    @Test
    void doesNotBroadcastWhenNothingReaped() {
        when(onlineUserService.reapStale(90_000L)).thenReturn(List.of());

        reaper.reap();

        verify(broadcastService, never()).broadcastToAll(argThat(
                env -> env != null && env.type() == ChatEventType.PRESENCE_SNAPSHOT));
    }
}
