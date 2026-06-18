package com.sef.cli.chat.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sef.cli.announcement.service.AnnouncementService;
import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.chat.event.ChatEnvelope;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.event.response.AnnouncementPayload;
import com.sef.cli.chat.event.response.ChatMessageBroadcast;
import com.sef.cli.chat.event.response.ErrorPayload;
import com.sef.cli.chat.event.response.PresenceSnapshotPayload;
import com.sef.cli.chat.event.response.RateLimitedPayload;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.chat.service.OnlineUserService;
import com.sef.cli.chat.service.RateLimiterService;
import com.sef.cli.message.entity.MessageEntity;
import com.sef.cli.message.enums.MessageType;
import com.sef.cli.message.service.MessageService;
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
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_USER_ID = "providerUserId";

    private final OnlineUserService onlineUserService;
    private final ChatBroadcastService broadcastService;
    private final MessageService messageService;
    private final AttendeeDataRepository attendeeDataRepository;
    private final ObjectMapper objectMapper;
    private final RateLimiterService rateLimiterService;
    private final AnnouncementService announcementService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String providerUserId = resolveProviderUserId(session);
        if (providerUserId == null) {
            log.warn("[WS_REJECT] 未授權的 /ws/chat 連線嘗試, sessionId={}", session.getId());
            session.close();
            return;
        }
        session.getAttributes().put(ATTR_USER_ID, providerUserId);

        onlineUserService.swap(providerUserId, session).ifPresent(oldSession -> {
            log.info("[WS_KICK_SWAP] 同 user 新連線踢掉舊連線, userId={}, oldSessionId={}, newSessionId={}",
                    providerUserId, oldSession.getId(), session.getId());
            broadcastService.sendTo(oldSession, new ChatEnvelope<>(ChatEventType.KICKED, System.currentTimeMillis(), null));
            try {
                oldSession.close(new CloseStatus(4271, "kicked"));
            } catch (IOException ex) {
                log.warn("failed to close old session id={} reason={}", oldSession.getId(), ex.getMessage());
            }
        });

        PresenceSnapshotPayload snapshot = new PresenceSnapshotPayload(new ArrayList<>(onlineUserService.getOnlineUserIds()));
        broadcastService.sendTo(session, new ChatEnvelope<>(ChatEventType.PRESENCE_SNAPSHOT, System.currentTimeMillis(), snapshot));
        // 晚到者補送目前公告（接於 presence snapshot 之後）；無公告則不送
        String announcement = announcementService.getCurrent();
        if (announcement != null) {
            broadcastService.sendTo(session, new ChatEnvelope<>(
                    ChatEventType.ANNOUNCEMENT, System.currentTimeMillis(), new AnnouncementPayload(announcement)));
        }
        broadcastService.broadcastToAll(new ChatEnvelope<>(ChatEventType.PRESENCE_SNAPSHOT, System.currentTimeMillis(), snapshot));
        log.info("[WS_CONNECT] 使用者連線, userId={}, sessionId={}, online={}",
                providerUserId, session.getId(), onlineUserService.getOnlineUserIds().size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = (String) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(message.getPayload());
        } catch (IOException ex) {
            sendError(session, "invalid_json", ex.getMessage());
            return;
        }

        String typeText = root.path("type").asText("");
        log.debug("[WS_IN] 入站 frame, userId={}, type={}", userId, typeText);
        ChatEventType type;
        try {
            type = ChatEventType.valueOf(typeText);
        } catch (IllegalArgumentException ex) {
            sendError(session, "unknown_type", "unknown event type: " + typeText);
            return;
        }

        switch (type) {
            case PING -> broadcastService.sendTo(session, new ChatEnvelope<>(ChatEventType.PONG, System.currentTimeMillis(), null));
            case CHAT_MESSAGE -> {
                long retryAfterMs = rateLimiterService.tryConsume(userId);
                if (retryAfterMs > 0) {
                    log.warn("[RATE_LIMITED] 訊息速率限制, userId={}, retryAfterMs={}", userId, retryAfterMs);
                    broadcastService.sendTo(session, new ChatEnvelope<>(
                            ChatEventType.RATE_LIMITED, System.currentTimeMillis(), new RateLimitedPayload(retryAfterMs)));
                } else {
                    handleChatMessage(session, userId, root.path("data"));
                }
            }
            default -> sendError(session, "unsupported_inbound_type", "event type not accepted inbound: " + type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            return;
        }
        onlineUserService.remove(userId, session);
        PresenceSnapshotPayload snapshot = new PresenceSnapshotPayload(new ArrayList<>(onlineUserService.getOnlineUserIds()));
        broadcastService.broadcastToAll(new ChatEnvelope<>(ChatEventType.PRESENCE_SNAPSHOT, System.currentTimeMillis(), snapshot));
        log.info("[WS_DISCONNECT] 使用者斷線, userId={}, sessionId={}, status={}, online={}",
                userId, session.getId(), status, onlineUserService.getOnlineUserIds().size());
    }

    private void handleChatMessage(WebSocketSession session, String userId, JsonNode data) {
        String messageTypeText = data.path("messageType").asText("");
        MessageType messageType;
        try {
            messageType = MessageType.valueOf(messageTypeText);
        } catch (IllegalArgumentException ex) {
            sendError(session, "unknown_message_type", "unknown messageType: " + messageTypeText);
            return;
        }

        if (messageType == MessageType.TEXT) {
            String content = data.path("content").isMissingNode() || data.path("content").isNull()
                    ? null : data.path("content").asText("");
            List<String> imageUrls = new ArrayList<>();
            if (data.path("imageUrls").isArray()) {
                data.path("imageUrls").forEach(node -> imageUrls.add(node.asText()));
            }
            persistAndBroadcast(session, userId, () -> messageService.persistText(userId, content, imageUrls));
            return;
        }

        if (messageType == MessageType.STICKER) {
            String stickerImageUrl = data.path("stickerImageUrl").isMissingNode() || data.path("stickerImageUrl").isNull()
                    ? null : data.path("stickerImageUrl").asText("");
            if (stickerImageUrl == null || !stickerImageUrl.startsWith("/sticker/")) {
                sendError(session, "sticker_image_url_invalid_prefix", "sticker url must start with /sticker/");
                return;
            }
            persistAndBroadcast(session, userId, () -> messageService.persistSticker(userId, stickerImageUrl));
            return;
        }

        sendError(session, "unsupported_message_type", "unsupported messageType: " + messageType);
    }

    private void persistAndBroadcast(WebSocketSession session, String userId,
                                     Supplier<MessageEntity> persist) {
        MessageEntity saved;
        try {
            saved = persist.get();
        } catch (IllegalArgumentException ex) {
            sendError(session, ex.getMessage(), ex.getMessage());
            return;
        }
        int contentLength = saved.getContent() == null ? 0 : saved.getContent().length();
        int imageCount = saved.getImageUrls() == null ? 0 : saved.getImageUrls().size();
        // 比照 Lizardchi [SAY]：記訊息原文 + contentLength/imageCount/stickerImageUrl（D7）
        log.info("[CHAT_MSG] 訊息發送, userId={}, messageType={}, messageId={}, content={}, contentLength={}, imageCount={}, stickerImageUrl={}",
                userId, saved.getMessageType(), saved.getMessageId(), saved.getContent(),
                contentLength, imageCount, saved.getStickerImageUrl());
        AttendeeDataEntity attendee = attendeeDataRepository.findByUserId(userId).orElse(null);
        ChatMessageBroadcast payload = new ChatMessageBroadcast(
                saved.getId(),
                saved.getMessageId(),
                saved.getUserId(),
                attendee == null ? null : attendee.getFurName(),
                attendee == null ? null : attendee.getAvatar(),
                attendee == null ? null : attendee.getAvatarColor(),
                attendee != null && attendee.isAvatarBorder(),
                saved.getMessageType(),
                saved.getContent(),
                saved.getImageUrls() == null ? List.of() : saved.getImageUrls(),
                saved.getStickerImageUrl(),
                saved.getCreatedDate());
        broadcastService.broadcastToAll(new ChatEnvelope<>(ChatEventType.CHAT_MESSAGE, System.currentTimeMillis(), payload));
    }

    private void sendError(WebSocketSession session, String code, String message) {
        log.warn("[WS_MSG_FAIL] 訊息處理失敗, code={}", code);
        broadcastService.sendTo(session, new ChatEnvelope<>(ChatEventType.ERROR, System.currentTimeMillis(), new ErrorPayload(code, message)));
    }

    private String resolveProviderUserId(WebSocketSession session) {
        Principal principal = session.getPrincipal();
        if (!(principal instanceof Authentication auth)) {
            return null;
        }
        if (!(auth.getPrincipal() instanceof AdminUserEntity user)) {
            return null;
        }
        return user.getProviderUserId();
    }
}
