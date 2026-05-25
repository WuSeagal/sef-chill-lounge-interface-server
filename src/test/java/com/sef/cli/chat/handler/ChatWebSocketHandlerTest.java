package com.sef.cli.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.chat.event.ChatEnvelope;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.chat.service.OnlineUserService;
import com.sef.cli.message.entity.MessageEntity;
import com.sef.cli.message.enums.MessageType;
import com.sef.cli.message.service.MessageService;
import com.sef.cli.user.entity.AdminUserEntity;
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
    private ChatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        onlineUserService = new OnlineUserService();
        broadcastService = mock(ChatBroadcastService.class);
        messageService = mock(MessageService.class);
        attendeeDataRepository = mock(AttendeeDataRepository.class);
        objectMapper = new ObjectMapper();
        handler = new ChatWebSocketHandler(onlineUserService, broadcastService, messageService, attendeeDataRepository, objectMapper);
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
    void rejectsStickerMessageInMvpSlice() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"CHAT_MESSAGE\",\"timestamp\":1,\"data\":{\"messageType\":\"STICKER\",\"content\":null,\"imageUrls\":[]}}"
        ));

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(broadcastService, atLeastOnce()).sendTo(eq(session), captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(typeIs(ChatEventType.ERROR))).isTrue();
        verify(messageService, never()).persistText(any(), any(), any());
    }

    @Test
    void afterConnectionClosedRemovesAndBroadcastsPresence() throws Exception {
        WebSocketSession session = mockAuthedSession("u-1");
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(onlineUserService.getOnlineUserIds()).isEmpty();
        verify(broadcastService, atLeastOnce()).broadcastToAll(argThat(envelopeOfType(ChatEventType.PRESENCE_SNAPSHOT)));
    }
}
