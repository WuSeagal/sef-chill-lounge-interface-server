package com.sef.cli.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sef.cli.chat.event.ChatEnvelope;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.event.response.PresenceSnapshotPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatBroadcastServiceTest {

    private OnlineUserService onlineUserService;
    private DashboardViewerService dashboardViewerService;
    private ChatBroadcastService broadcastService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        onlineUserService = mock(OnlineUserService.class);
        dashboardViewerService = mock(DashboardViewerService.class);
        broadcastService = new ChatBroadcastService(onlineUserService, dashboardViewerService, objectMapper);
    }

    @Test
    void sendToSerializesEnvelopeAndCallsSessionSendMessage() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        ChatEnvelope<PresenceSnapshotPayload> envelope = new ChatEnvelope<>(
                ChatEventType.PRESENCE_SNAPSHOT, 1L, new PresenceSnapshotPayload(List.of("u-1"))
        );

        broadcastService.sendTo(session, envelope);

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcastToAllSendsToEverySession() throws IOException {
        WebSocketSession s1 = mock(WebSocketSession.class);
        WebSocketSession s2 = mock(WebSocketSession.class);
        when(s1.isOpen()).thenReturn(true);
        when(s2.isOpen()).thenReturn(true);
        when(onlineUserService.getAllSessions()).thenReturn(List.of(s1, s2));

        broadcastService.broadcastToAll(new ChatEnvelope<>(ChatEventType.PRESENCE_SNAPSHOT, 1L, new PresenceSnapshotPayload(List.of())));

        verify(s1).sendMessage(any(TextMessage.class));
        verify(s2).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcastSingleSessionFailureDoesNotStopOthers() throws IOException {
        WebSocketSession bad = mock(WebSocketSession.class);
        WebSocketSession good = mock(WebSocketSession.class);
        when(bad.isOpen()).thenReturn(true);
        when(good.isOpen()).thenReturn(true);
        doThrow(new IOException("broken pipe")).when(bad).sendMessage(any(TextMessage.class));
        when(onlineUserService.getAllSessions()).thenReturn(List.of(bad, good));

        broadcastService.broadcastToAll(new ChatEnvelope<>(ChatEventType.PRESENCE_SNAPSHOT, 1L, new PresenceSnapshotPayload(List.of())));

        verify(good).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendToSkipsClosedSession() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(false);

        broadcastService.sendTo(session, new ChatEnvelope<>(ChatEventType.PONG, 1L, null));

        verify(session, never()).sendMessage(any());
    }

    @Test
    void broadcastToAllAlsoSendsToDashboardViewers() throws IOException {
        WebSocketSession chat = mock(WebSocketSession.class);
        WebSocketSession viewer = mock(WebSocketSession.class);
        when(chat.isOpen()).thenReturn(true);
        when(viewer.isOpen()).thenReturn(true);
        when(onlineUserService.getAllSessions()).thenReturn(List.of(chat));
        when(dashboardViewerService.getAllSessions()).thenReturn(List.of(viewer));

        broadcastService.broadcastToAll(new ChatEnvelope<>(ChatEventType.CHAT_MESSAGE, 1L, null));

        verify(chat).sendMessage(any(TextMessage.class));
        verify(viewer).sendMessage(any(TextMessage.class));
    }
}
