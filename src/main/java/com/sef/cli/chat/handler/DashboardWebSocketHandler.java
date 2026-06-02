package com.sef.cli.chat.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sef.cli.chat.event.ChatEnvelope;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.event.response.ChatMessageBroadcast;
import com.sef.cli.chat.event.response.PresenceSnapshotPayload;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.chat.service.DashboardViewerService;
import com.sef.cli.chat.service.OnlineUserService;
import com.sef.cli.message.service.MessageService;
import com.sef.cli.message.service.dto.MessageHistoryData;
import com.sef.cli.user.entity.AdminUserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only dashboard viewer endpoint (/ws/dashboard). Registers the session as a
 * passive viewer (no swap, not part of presence — many viewers per user coexist),
 * replays the most recent messages on connect, answers PING heartbeats, and ignores
 * every other inbound frame. CHAT_MESSAGE / PROFILE_UPDATED reach viewers via
 * ChatBroadcastService.broadcastToAll fan-out — NOT handled here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private static final int REPLAY_LIMIT = 30;

    private final DashboardViewerService viewerService;
    private final ChatBroadcastService broadcastService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final OnlineUserService onlineUserService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!isAuthenticated(session)) {
            session.close();
            return;
        }
        // Register before replay so live messages arriving mid-replay are also delivered
        // (frontend dedups by messageId). On replay failure, undo the registration so a
        // dead viewer is not left behind.
        viewerService.register(session);
        try {
            replayRecentHistory(session);
            sendPresenceSnapshot(session);
        } catch (RuntimeException ex) {
            viewerService.unregister(session);
            log.warn("dashboard replay failed, unregistering viewer sessionId={} reason={}", session.getId(), ex.getMessage());
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        JsonNode root;
        try {
            root = objectMapper.readTree(message.getPayload());
        } catch (IOException ex) {
            return; // read-only: silently ignore malformed inbound
        }
        if ("PING".equals(root.path("type").asText(""))) {
            broadcastService.sendTo(session, new ChatEnvelope<>(ChatEventType.PONG, System.currentTimeMillis(), null));
        }
        // every other inbound type (incl. CHAT_MESSAGE) is ignored — viewer is read-only
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        viewerService.unregister(session);
    }

    private void sendPresenceSnapshot(WebSocketSession session) {
        PresenceSnapshotPayload snapshot = new PresenceSnapshotPayload(new ArrayList<>(onlineUserService.getOnlineUserIds()));
        broadcastService.sendTo(session, new ChatEnvelope<>(ChatEventType.PRESENCE_SNAPSHOT, System.currentTimeMillis(), snapshot));
    }

    private void replayRecentHistory(WebSocketSession session) {
        List<MessageHistoryData> recent = messageService.loadHistory(null, null, REPLAY_LIMIT);
        // loadHistory returns newest-first; replay oldest-first so bubbles arrive in order
        for (int i = recent.size() - 1; i >= 0; i--) {
            MessageHistoryData d = recent.get(i);
            ChatMessageBroadcast payload = new ChatMessageBroadcast(
                    d.cursorId(), d.messageId(), d.userId(), d.furName(), d.avatar(),
                    d.avatarColor(), d.avatarBorder(), d.messageType(), d.content(),
                    d.imageUrls(), d.stickerImageUrl(), d.createdDate());
            broadcastService.sendTo(session, new ChatEnvelope<>(ChatEventType.CHAT_MESSAGE, System.currentTimeMillis(), payload));
        }
    }

    private boolean isAuthenticated(WebSocketSession session) {
        Principal principal = session.getPrincipal();
        return principal instanceof Authentication auth && auth.getPrincipal() instanceof AdminUserEntity;
    }
}
