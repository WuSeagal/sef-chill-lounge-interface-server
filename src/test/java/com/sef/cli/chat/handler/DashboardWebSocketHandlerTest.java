package com.sef.cli.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sef.cli.chat.event.ChatEnvelope;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.chat.service.DashboardViewerService;
import com.sef.cli.message.enums.MessageType;
import com.sef.cli.message.service.MessageService;
import com.sef.cli.message.service.dto.MessageHistoryData;
import com.sef.cli.user.entity.AdminUserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardWebSocketHandlerTest {

    private DashboardViewerService viewerService;
    private ChatBroadcastService broadcastService;
    private MessageService messageService;
    private ObjectMapper objectMapper;
    private DashboardWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        viewerService = mock(DashboardViewerService.class);
        broadcastService = mock(ChatBroadcastService.class);
        messageService = mock(MessageService.class);
        objectMapper = new ObjectMapper();
        handler = new DashboardWebSocketHandler(viewerService, broadcastService, messageService, objectMapper);
    }

    private WebSocketSession authedSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        AdminUserEntity user = AdminUserEntity.builder()
                .providerUserId("viewer-1").roleName("USER").enabled(true)
                .firstLogin(false).banned(false).build();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        when(session.getPrincipal()).thenReturn((Principal) auth);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private static Predicate<ChatEnvelope<?>> typeIs(ChatEventType type) {
        return env -> env.type() == type;
    }

    @Test
    void closesSessionWhenPrincipalMissing() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getPrincipal()).thenReturn(null);

        handler.afterConnectionEstablished(session);

        verify(session).close();
        verify(viewerService, never()).register(any());
    }

    @Test
    void afterConnectionEstablishedRegistersViewer() throws Exception {
        WebSocketSession session = authedSession();
        when(messageService.loadHistory(null, null, 30)).thenReturn(List.of());

        handler.afterConnectionEstablished(session);

        verify(viewerService).register(session);
    }

    @Test
    void replaysRecentHistoryOldestFirstAsChatMessages() throws Exception {
        WebSocketSession session = authedSession();
        MessageHistoryData newer = new MessageHistoryData(2L, "msg-2", "u-1", MessageType.TEXT,
                "Fox", "/a.png", "#fff", false, "second", List.of(), null, LocalDateTime.now());
        MessageHistoryData older = new MessageHistoryData(1L, "msg-1", "u-1", MessageType.TEXT,
                "Fox", "/a.png", "#fff", false, "first", List.of(), null, LocalDateTime.now());
        when(messageService.loadHistory(null, null, 30)).thenReturn(List.of(newer, older));

        handler.afterConnectionEstablished(session);

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).sendTo(eq(session), captor.capture());
        List<ChatEnvelope<?>> chatMsgs = captor.getAllValues().stream().filter(typeIs(ChatEventType.CHAT_MESSAGE)).toList();
        assertThat(chatMsgs).hasSize(2);
        assertThat(((com.sef.cli.chat.event.response.ChatMessageBroadcast) chatMsgs.get(0).data()).messageId()).isEqualTo("msg-1");
        assertThat(((com.sef.cli.chat.event.response.ChatMessageBroadcast) chatMsgs.get(1).data()).messageId()).isEqualTo("msg-2");
    }

    @Test
    void handlePingRepliesPong() throws Exception {
        WebSocketSession session = authedSession();

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"PING\",\"timestamp\":1,\"data\":null}"));

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).sendTo(eq(session), captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(typeIs(ChatEventType.PONG))).isTrue();
    }

    @Test
    void ignoresInboundChatMessage_readOnly() throws Exception {
        WebSocketSession session = authedSession();

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"CHAT_MESSAGE\",\"timestamp\":1,\"data\":{\"messageType\":\"TEXT\",\"content\":\"hi\",\"imageUrls\":[]}}"));

        verify(messageService, never()).persistText(any(), any(), any());
        verify(broadcastService, never()).broadcastToAll(any());
    }

    @Test
    void afterConnectionClosedUnregistersViewer() throws Exception {
        WebSocketSession session = authedSession();

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(viewerService).unregister(session);
    }

    @Test
    void unregistersViewerAndClosesWhenReplayFails() throws Exception {
        WebSocketSession session = authedSession();
        when(messageService.loadHistory(null, null, 30)).thenThrow(new RuntimeException("db down"));

        handler.afterConnectionEstablished(session);

        verify(viewerService).register(session);
        verify(viewerService).unregister(session);
        verify(session).close();
    }
}
