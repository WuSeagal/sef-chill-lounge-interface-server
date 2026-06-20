package com.sef.cli.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sef.cli.chat.event.ChatEnvelope;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.event.response.PresenceSnapshotPayload;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.chat.service.DashboardViewerService;
import com.sef.cli.chat.service.OnlineUserService;
import com.sef.cli.common.BanGuard;
import com.sef.cli.message.enums.MessageType;
import com.sef.cli.message.service.MessageService;
import com.sef.cli.message.service.dto.MessageHistoryData;
import com.sef.cli.testutil.LogCaptor;
import com.sef.cli.user.entity.AdminUserEntity;
import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    private OnlineUserService onlineUserService;
    private BanGuard banGuard;
    private DashboardWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        viewerService = mock(DashboardViewerService.class);
        broadcastService = mock(ChatBroadcastService.class);
        messageService = mock(MessageService.class);
        objectMapper = new ObjectMapper();
        onlineUserService = new OnlineUserService();
        banGuard = mock(BanGuard.class); // 預設 isBanned=false
        handler = new DashboardWebSocketHandler(viewerService, broadcastService, messageService, objectMapper, onlineUserService, banGuard);
    }

    private WebSocketSession authedSession() {
        return authedSession("viewer-1");
    }

    private WebSocketSession authedSession(String providerUserId) {
        WebSocketSession session = mock(WebSocketSession.class);
        AdminUserEntity user = AdminUserEntity.builder()
                .providerUserId(providerUserId).roleName("USER").enabled(true)
                .firstLogin(false).banned(false).build();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        when(session.getPrincipal()).thenReturn((Principal) auth);
        when(session.isOpen()).thenReturn(true);
        // D5：handler 連線時將 decorator 存入 attributes，需可寫入。
        when(session.getAttributes()).thenReturn(new HashMap<>());
        return session;
    }

    private static ArgumentMatcher<WebSocketSession> isDecorator() {
        return s -> s instanceof ConcurrentWebSocketSessionDecorator;
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
    void closesSessionAndSkipsRegisterWhenUserBanned() throws Exception {
        WebSocketSession session = authedSession("banned-viewer");
        when(banGuard.isBanned("banned-viewer")).thenReturn(true);

        handler.afterConnectionEstablished(session);

        ArgumentCaptor<CloseStatus> closeStatusCaptor = ArgumentCaptor.forClass(CloseStatus.class);
        verify(session).close(closeStatusCaptor.capture());
        assertThat(closeStatusCaptor.getValue().getCode()).isEqualTo(4403);
        verify(viewerService, never()).register(any());
    }

    @Test
    void afterConnectionEstablishedRegistersViewer() throws Exception {
        WebSocketSession session = authedSession();
        when(messageService.loadHistory(null, null, 30)).thenReturn(List.of());

        handler.afterConnectionEstablished(session);

        // D5：註冊的是包裝後的 decorator，非原始 session。
        verify(viewerService).register(argThat(isDecorator()));
    }

    @Test
    void registerStoresDecoratorWrappingOriginalSession() throws Exception {
        WebSocketSession session = authedSession();
        when(messageService.loadHistory(null, null, 30)).thenReturn(List.of());

        handler.afterConnectionEstablished(session);

        ArgumentCaptor<WebSocketSession> captor = ArgumentCaptor.forClass(WebSocketSession.class);
        verify(viewerService).register(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ConcurrentWebSocketSessionDecorator.class);
        assertThat(((ConcurrentWebSocketSessionDecorator) captor.getValue()).getDelegate()).isSameAs(session);
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
        // 連線初始 replay 改走 decorated（D5：避免與並發 broadcastToAll 對同一 socket 競寫）。
        verify(broadcastService, atLeastOnce()).sendTo(argThat(isDecorator()), captor.capture());
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
        // establish-first 讓 decorator 存入 attributes，afterConnectionClosed 才能取回同一實例 unregister。
        WebSocketSession session = authedSession();
        when(messageService.loadHistory(null, null, 30)).thenReturn(List.of());
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // unregister 兩次：register 用 decorator；close 也應以同一 decorator unregister。
        verify(viewerService, atLeastOnce()).unregister(argThat(isDecorator()));
    }

    @Test
    void unregistersViewerAndClosesWhenReplayFails() throws Exception {
        WebSocketSession session = authedSession();
        when(messageService.loadHistory(null, null, 30)).thenThrow(new RuntimeException("db down"));

        handler.afterConnectionEstablished(session);

        // register 與失敗後的 unregister 都必須是同一 decorator 實例。
        verify(viewerService).register(argThat(isDecorator()));
        verify(viewerService).unregister(argThat(isDecorator()));
        verify(session).close();
    }

    @Test
    void sendsInitialPresenceSnapshotToViewerOnConnect() throws Exception {
        onlineUserService.swap("u-1", mock(WebSocketSession.class));
        onlineUserService.swap("u-2", mock(WebSocketSession.class));
        when(messageService.loadHistory(null, null, 30)).thenReturn(List.of());
        WebSocketSession viewer = authedSession();

        handler.afterConnectionEstablished(viewer);

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        // 連線初始 snapshot 改走 decorated（D5）。
        verify(broadcastService, atLeastOnce()).sendTo(argThat(isDecorator()), captor.capture());
        ChatEnvelope<?> snap = captor.getAllValues().stream()
                .filter(typeIs(ChatEventType.PRESENCE_SNAPSHOT))
                .findFirst().orElseThrow();
        PresenceSnapshotPayload payload = (PresenceSnapshotPayload) snap.data();
        assertThat(payload.onlineUserIds()).containsExactlyInAnyOrder("u-1", "u-2");
    }

    // ---- 行為 log 斷言（backend-behavior-logging section 5.1）----

    @Test
    void connect_logsDashConnectInfo() throws Exception {
        WebSocketSession session = authedSession();
        when(messageService.loadHistory(null, null, 30)).thenReturn(List.of());
        try (LogCaptor captor = LogCaptor.forClass(DashboardWebSocketHandler.class)) {
            handler.afterConnectionEstablished(session);
            captor.assertLogged(Level.INFO, "[DASH_CONNECT]", "viewers=");
        }
    }

    @Test
    void disconnect_logsDashDisconnectInfo() throws Exception {
        WebSocketSession session = authedSession();
        try (LogCaptor captor = LogCaptor.forClass(DashboardWebSocketHandler.class)) {
            handler.afterConnectionClosed(session, CloseStatus.NORMAL);
            captor.assertLogged(Level.INFO, "[DASH_DISCONNECT]");
        }
    }

    @Test
    void reject_unauthenticated_logsWarn() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getPrincipal()).thenReturn(null);
        try (LogCaptor captor = LogCaptor.forClass(DashboardWebSocketHandler.class)) {
            handler.afterConnectionEstablished(session);
            captor.assertLogged(Level.WARN, "[DASH_REJECT]");
        }
    }
}
