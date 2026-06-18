package com.sef.cli.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sef.cli.announcement.service.AnnouncementService;
import com.sef.cli.chat.event.response.AnnouncementPayload;
import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.chat.event.ChatEnvelope;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.event.response.ChatMessageBroadcast;
import com.sef.cli.chat.event.response.TypingPayload;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.chat.service.OnlineUserService;
import com.sef.cli.chat.service.RateLimiterService;
import com.sef.cli.message.entity.MessageEntity;
import com.sef.cli.message.enums.MessageType;
import com.sef.cli.message.service.MessageService;
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

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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

class ChatWebSocketHandlerTest {

    private OnlineUserService onlineUserService;
    private ChatBroadcastService broadcastService;
    private MessageService messageService;
    private AttendeeDataRepository attendeeDataRepository;
    private ObjectMapper objectMapper;
    private RateLimiterService rateLimiterService;
    private AnnouncementService announcementService;
    private ChatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        onlineUserService = new OnlineUserService();
        broadcastService = mock(ChatBroadcastService.class);
        messageService = mock(MessageService.class);
        attendeeDataRepository = mock(AttendeeDataRepository.class);
        objectMapper = new ObjectMapper();
        rateLimiterService = mock(RateLimiterService.class);
        announcementService = mock(AnnouncementService.class);
        // 未 stub 時 tryConsume 回 Mockito 預設 0L = 放行；getCurrent() 預設 null = 無公告。
        handler = new ChatWebSocketHandler(onlineUserService, broadcastService, messageService, attendeeDataRepository, objectMapper, rateLimiterService, announcementService);
    }

    private WebSocketSession mockAuthedSession(String providerUserId) {
        WebSocketSession session = mock(WebSocketSession.class);
        AdminUserEntity user = AdminUserEntity.builder()
                .providerUserId(providerUserId)
                .roleName("USER")
                .enabled(true)
                .firstLogin(false)
                .banned(false)
                .build();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        when(session.getPrincipal()).thenReturn((Principal) auth);
        when(session.getAttributes()).thenReturn(new HashMap<>());
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private static ArgumentMatcher<ChatEnvelope<?>> envelopeOfType(ChatEventType type) {
        return env -> env != null && env.type() == type;
    }

    private static Predicate<ChatEnvelope<?>> typeIs(ChatEventType type) {
        return env -> env.type() == type;
    }

    @Test
    void closesSessionWhenPrincipalIsMissing() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getPrincipal()).thenReturn(null);

        handler.afterConnectionEstablished(session);

        verify(session).close();
        assertThat(onlineUserService.getOnlineUserIds()).isEmpty();
    }

    @Test
    void afterConnectionEstablishedRegistersUserAndBroadcastsPresence() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");

        handler.afterConnectionEstablished(session);

        assertThat(onlineUserService.getOnlineUserIds()).containsExactly("u-1");
        verify(broadcastService).sendTo(eq(session), any());
        verify(broadcastService).broadcastToAll(any());
    }

    @Test
    void connectSendsCurrentAnnouncementWhenPresent() throws Exception {
        when(announcementService.getCurrent()).thenReturn("公告X");
        WebSocketSession session = mockAuthedSession("u-1");

        handler.afterConnectionEstablished(session);

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).sendTo(eq(session), captor.capture());
        List<ChatEnvelope<?>> sent = captor.getAllValues();
        assertThat(sent.stream().anyMatch(e ->
                e.type() == ChatEventType.ANNOUNCEMENT
                        && e.data() instanceof AnnouncementPayload p
                        && "公告X".equals(p.text()))).isTrue();
        // 須接於 presence snapshot 之後（spec/D1）
        int presenceIdx = -1;
        int announcementIdx = -1;
        for (int i = 0; i < sent.size(); i++) {
            if (presenceIdx == -1 && sent.get(i).type() == ChatEventType.PRESENCE_SNAPSHOT) presenceIdx = i;
            if (sent.get(i).type() == ChatEventType.ANNOUNCEMENT) announcementIdx = i;
        }
        assertThat(presenceIdx).isGreaterThanOrEqualTo(0);
        assertThat(announcementIdx).isGreaterThan(presenceIdx);
    }

    @Test
    void connectDoesNotSendAnnouncementWhenAbsent() throws Exception {
        // announcementService.getCurrent() 預設回 null
        WebSocketSession session = mockAuthedSession("u-1");

        handler.afterConnectionEstablished(session);

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).sendTo(eq(session), captor.capture());
        assertThat(captor.getAllValues().stream().noneMatch(typeIs(ChatEventType.ANNOUNCEMENT))).isTrue();
    }

    @Test
    void newConnectionKicksOldSessionWithCode4271() throws Exception {
        WebSocketSession oldSession = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(oldSession);

        WebSocketSession newSession = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(newSession);

        ArgumentCaptor<CloseStatus> closeStatusCaptor = ArgumentCaptor.forClass(CloseStatus.class);
        verify(oldSession).close(closeStatusCaptor.capture());
        assertThat(closeStatusCaptor.getValue().getCode()).isEqualTo(4271);
        verify(broadcastService, atLeastOnce()).sendTo(eq(oldSession), argThat(envelopeOfType(ChatEventType.KICKED)));
    }

    @Test
    void handlePingRepliesWithPong() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"PING\",\"timestamp\":1,\"data\":null}"));

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).sendTo(eq(session), captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(typeIs(ChatEventType.PONG))).isTrue();
    }

    @Test
    void handleChatMessageTextPersistsAndBroadcasts() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);

        when(messageService.persistText(eq("u-1"), eq("hello"), eq(List.of()))).thenReturn(
                MessageEntity.builder()
                        .id(1L)
                        .messageId("msg-001")
                        .userId("u-1")
                        .messageType(MessageType.TEXT)
                        .content("hello")
                        .createdDate(LocalDateTime.now())
                        .build()
        );
        when(attendeeDataRepository.findByUserId("u-1")).thenReturn(Optional.of(
                AttendeeDataEntity.builder().userId("u-1").furName("Fox").avatar("/a.png").build()
        ));

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"CHAT_MESSAGE\",\"timestamp\":1,\"data\":{\"messageType\":\"TEXT\",\"content\":\"hello\",\"imageUrls\":[]}}"
        ));

        verify(messageService).persistText("u-1", "hello", List.of());
        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).broadcastToAll(captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(typeIs(ChatEventType.CHAT_MESSAGE))).isTrue();
    }

    @Test
    void chatMessageBroadcastCarriesAvatarColorAndBorder() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);

        when(messageService.persistText(eq("u-1"), eq("hello"), any())).thenReturn(
                MessageEntity.builder()
                        .id(1L).messageId("msg-001").userId("u-1")
                        .messageType(MessageType.TEXT).content("hello")
                        .createdDate(LocalDateTime.now()).build()
        );
        when(attendeeDataRepository.findByUserId("u-1")).thenReturn(Optional.of(
                AttendeeDataEntity.builder().userId("u-1").furName("Fox").avatar("/a.png")
                        .avatarColor("#7b9b8f").avatarBorder(true).build()
        ));

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"CHAT_MESSAGE\",\"timestamp\":1,\"data\":{\"messageType\":\"TEXT\",\"content\":\"hello\",\"imageUrls\":[]}}"
        ));

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).broadcastToAll(captor.capture());
        ChatMessageBroadcast payload = (ChatMessageBroadcast) captor.getAllValues().stream()
                .filter(typeIs(ChatEventType.CHAT_MESSAGE))
                .findFirst().orElseThrow().data();
        assertThat(payload.avatarColor()).isEqualTo("#7b9b8f");
        assertThat(payload.avatarBorder()).isTrue();
    }

    @Test
    void rateLimitedChatMessageSendsRateLimitedAndDoesNotPersist() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);
        when(rateLimiterService.tryConsume("u-1")).thenReturn(5_000L);

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"CHAT_MESSAGE\",\"timestamp\":1,\"data\":{\"messageType\":\"TEXT\",\"content\":\"hi\",\"imageUrls\":[]}}"));

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).sendTo(eq(session), captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(typeIs(ChatEventType.RATE_LIMITED))).isTrue();
        verify(messageService, never()).persistText(any(), any(), any());
        verify(broadcastService, never()).broadcastToAll(argThat(envelopeOfType(ChatEventType.CHAT_MESSAGE)));
    }

    @Test
    void handleChatMessageValidationFailureSendsErrorAndDoesNotPersist() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);

        when(messageService.persistText(eq("u-1"), any(), any()))
                .thenThrow(new IllegalArgumentException("message_content_too_long"));

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"CHAT_MESSAGE\",\"timestamp\":1,\"data\":{\"messageType\":\"TEXT\",\"content\":\"x\",\"imageUrls\":[]}}"
        ));

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).sendTo(eq(session), captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(typeIs(ChatEventType.ERROR))).isTrue();
        verify(broadcastService, never()).broadcastToAll(argThat(envelopeOfType(ChatEventType.CHAT_MESSAGE)));
    }

    @Test
    void handleStickerMessagePersistsAndBroadcasts() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);

        when(messageService.persistSticker(eq("u-1"), eq("/sticker/u-1/1.png?v=1"))).thenReturn(
                MessageEntity.builder()
                        .id(2L).messageId("msg-002").userId("u-1")
                        .messageType(MessageType.STICKER).stickerImageUrl("/sticker/u-1/1.png?v=1")
                        .createdDate(LocalDateTime.now()).build());
        when(attendeeDataRepository.findByUserId("u-1")).thenReturn(Optional.of(
                AttendeeDataEntity.builder().userId("u-1").furName("Fox").build()));

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"CHAT_MESSAGE\",\"timestamp\":1,\"data\":{\"messageType\":\"STICKER\",\"stickerImageUrl\":\"/sticker/u-1/1.png?v=1\"}}"));

        verify(messageService).persistSticker("u-1", "/sticker/u-1/1.png?v=1");
        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).broadcastToAll(captor.capture());
        ChatMessageBroadcast payload = (ChatMessageBroadcast) captor.getAllValues().stream()
                .filter(typeIs(ChatEventType.CHAT_MESSAGE)).findFirst().orElseThrow().data();
        assertThat(payload.stickerImageUrl()).isEqualTo("/sticker/u-1/1.png?v=1");
    }

    @Test
    void handleStickerMessageRejectsInvalidPrefix() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"CHAT_MESSAGE\",\"timestamp\":1,\"data\":{\"messageType\":\"STICKER\",\"stickerImageUrl\":\"/evil/x.png\"}}"));

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).sendTo(eq(session), captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(typeIs(ChatEventType.ERROR))).isTrue();
        verify(messageService, never()).persistSticker(any(), any());
    }

    @Test
    void handleUnknownEnvelopeTypeRepliesWithError() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"GARBAGE\",\"timestamp\":1,\"data\":null}"));

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).sendTo(eq(session), captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(typeIs(ChatEventType.ERROR))).isTrue();
        verify(messageService, never()).persistText(any(), any(), any());
    }

    @Test
    void handleInvalidJsonRepliesWithError() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("not-valid-json"));

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).sendTo(eq(session), captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(typeIs(ChatEventType.ERROR))).isTrue();
        verify(messageService, never()).persistText(any(), any(), any());
    }

    // ---- TYPING 輸入中廣播 ----

    @Test
    void handleTypingBroadcastsToAllWithDisplayFields() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);
        when(attendeeDataRepository.findByUserId("u-1")).thenReturn(Optional.of(
                AttendeeDataEntity.builder().userId("u-1").furName("Fox").avatar("/a.png").avatarColor("#7b9b8f").build()));

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"TYPING\",\"timestamp\":1,\"data\":null}"));

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).broadcastToAll(captor.capture());
        TypingPayload payload = (TypingPayload) captor.getAllValues().stream()
                .filter(typeIs(ChatEventType.TYPING)).findFirst().orElseThrow().data();
        assertThat(payload.userId()).isEqualTo("u-1");
        assertThat(payload.furName()).isEqualTo("Fox");
        assertThat(payload.avatar()).isEqualTo("/a.png");
        assertThat(payload.avatarColor()).isEqualTo("#7b9b8f");
    }

    @Test
    void typingDoesNotConsumeRateLimitOrPersist() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"TYPING\",\"timestamp\":1,\"data\":null}"));

        verify(rateLimiterService, never()).tryConsume(any());
        verify(messageService, never()).persistText(any(), any(), any());
        verify(messageService, never()).persistSticker(any(), any());
    }

    @Test
    void typingNotRejectedAsUnsupportedInbound() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"TYPING\",\"timestamp\":1,\"data\":null}"));

        // 不得回任何 ERROR envelope（尤其不得是 unsupported_inbound_type）
        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).sendTo(eq(session), captor.capture());
        assertThat(captor.getAllValues().stream().noneMatch(typeIs(ChatEventType.ERROR))).isTrue();
    }

    @Test
    void typingWithUnknownAttendeeBroadcastsNullDisplayFields() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);
        when(attendeeDataRepository.findByUserId("u-1")).thenReturn(Optional.empty());

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"TYPING\",\"timestamp\":1,\"data\":null}"));

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).broadcastToAll(captor.capture());
        TypingPayload payload = (TypingPayload) captor.getAllValues().stream()
                .filter(typeIs(ChatEventType.TYPING)).findFirst().orElseThrow().data();
        assertThat(payload.userId()).isEqualTo("u-1");
        assertThat(payload.furName()).isNull();
        assertThat(payload.avatar()).isNull();
        assertThat(payload.avatarColor()).isNull();
    }

    @Test
    void afterConnectionClosedRemovesAndBroadcastsPresence() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(onlineUserService.getOnlineUserIds()).isEmpty();
        verify(broadcastService, atLeastOnce()).broadcastToAll(argThat(envelopeOfType(ChatEventType.PRESENCE_SNAPSHOT)));
    }

    // ---- 行為 log 斷言（backend-behavior-logging section 4.1）----

    @Test
    void connect_logsInfoWithOnlineCount() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        try (LogCaptor captor = LogCaptor.forClass(ChatWebSocketHandler.class)) {
            handler.afterConnectionEstablished(session);
            captor.assertLogged(Level.INFO, "[WS_CONNECT]", "userId=u-1", "online=1");
        }
    }

    @Test
    void reject_unauthenticated_logsWarn() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getPrincipal()).thenReturn(null);
        try (LogCaptor captor = LogCaptor.forClass(ChatWebSocketHandler.class)) {
            handler.afterConnectionEstablished(session);
            captor.assertLogged(Level.WARN, "[WS_REJECT]");
        }
    }

    @Test
    void swap_kicksOldSession_logsInfo() throws Exception {
        handler.afterConnectionEstablished(mockAuthedSession("u-1"));
        try (LogCaptor captor = LogCaptor.forClass(ChatWebSocketHandler.class)) {
            handler.afterConnectionEstablished(mockAuthedSession("u-1"));
            captor.assertLogged(Level.INFO, "[WS_KICK_SWAP]", "userId=u-1");
        }
    }

    @Test
    void disconnect_logsInfo() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);
        try (LogCaptor captor = LogCaptor.forClass(ChatWebSocketHandler.class)) {
            handler.afterConnectionClosed(session, CloseStatus.NORMAL);
            captor.assertLogged(Level.INFO, "[WS_DISCONNECT]", "userId=u-1");
        }
    }

    @Test
    void rateLimited_logsWarn() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);
        when(rateLimiterService.tryConsume("u-1")).thenReturn(5_000L);
        try (LogCaptor captor = LogCaptor.forClass(ChatWebSocketHandler.class)) {
            handler.handleTextMessage(session, new TextMessage(
                    "{\"type\":\"CHAT_MESSAGE\",\"timestamp\":1,\"data\":{\"messageType\":\"TEXT\",\"content\":\"hi\",\"imageUrls\":[]}}"));
            captor.assertLogged(Level.WARN, "[RATE_LIMITED]", "userId=u-1", "retryAfterMs=5000");
        }
    }

    @Test
    void textMessage_logsChatMsgInfoWithRawContent() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);
        when(messageService.persistText(eq("u-1"), eq("hello"), eq(List.of()))).thenReturn(
                MessageEntity.builder().id(1L).messageId("msg-001").userId("u-1")
                        .messageType(MessageType.TEXT).content("hello").createdDate(LocalDateTime.now()).build());
        when(attendeeDataRepository.findByUserId("u-1")).thenReturn(Optional.of(
                AttendeeDataEntity.builder().userId("u-1").furName("Fox").build()));

        try (LogCaptor captor = LogCaptor.forClass(ChatWebSocketHandler.class)) {
            handler.handleTextMessage(session, new TextMessage(
                    "{\"type\":\"CHAT_MESSAGE\",\"timestamp\":1,\"data\":{\"messageType\":\"TEXT\",\"content\":\"hello\",\"imageUrls\":[]}}"));
            // 比照 Lizardchi 記原文 hello + contentLength=5 + imageCount=0
            captor.assertLogged(Level.INFO, "[CHAT_MSG]", "userId=u-1", "messageId=msg-001",
                    "content=hello", "contentLength=5", "imageCount=0");
        }
    }

    @Test
    void stickerMessage_logsChatMsgInfoWithStickerUrl() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);
        when(messageService.persistSticker(eq("u-1"), eq("/sticker/u-1/1.png?v=1"))).thenReturn(
                MessageEntity.builder().id(2L).messageId("msg-002").userId("u-1")
                        .messageType(MessageType.STICKER).stickerImageUrl("/sticker/u-1/1.png?v=1")
                        .createdDate(LocalDateTime.now()).build());
        when(attendeeDataRepository.findByUserId("u-1")).thenReturn(Optional.of(
                AttendeeDataEntity.builder().userId("u-1").furName("Fox").build()));

        try (LogCaptor captor = LogCaptor.forClass(ChatWebSocketHandler.class)) {
            handler.handleTextMessage(session, new TextMessage(
                    "{\"type\":\"CHAT_MESSAGE\",\"timestamp\":1,\"data\":{\"messageType\":\"STICKER\",\"stickerImageUrl\":\"/sticker/u-1/1.png?v=1\"}}"));
            captor.assertLogged(Level.INFO, "[CHAT_MSG]", "userId=u-1",
                    "messageId=msg-002", "stickerImageUrl=/sticker/u-1/1.png?v=1");
        }
    }

    @Test
    void validationFailure_logsMsgFailWarn() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);
        try (LogCaptor captor = LogCaptor.forClass(ChatWebSocketHandler.class)) {
            handler.handleTextMessage(session, new TextMessage("{\"type\":\"GARBAGE\",\"timestamp\":1,\"data\":null}"));
            captor.assertLogged(Level.WARN, "[WS_MSG_FAIL]", "code=unknown_type");
        }
    }

    @Test
    void inboundFrame_logsDebug() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);
        try (LogCaptor captor = LogCaptor.forClass(ChatWebSocketHandler.class)) {
            handler.handleTextMessage(session, new TextMessage("{\"type\":\"PING\",\"timestamp\":1,\"data\":null}"));
            captor.assertLogged(Level.DEBUG, "[WS_IN]", "userId=u-1", "type=PING");
        }
    }
}
