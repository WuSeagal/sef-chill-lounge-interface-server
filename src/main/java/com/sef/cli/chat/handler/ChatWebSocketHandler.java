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
import com.sef.cli.chat.event.response.TypingPayload;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.chat.service.OnlineUserService;
import com.sef.cli.chat.service.RateLimiterService;
import com.sef.cli.common.BanGuard;
import com.sef.cli.message.entity.MessageEntity;
import com.sef.cli.message.enums.MessageType;
import com.sef.cli.message.service.MessageService;
import com.sef.cli.message.service.dto.ReplyPreview;
import com.sef.cli.user.entity.AdminUserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
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
    private static final String ATTR_WS_DECORATED = "wsDecorated";
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024;

    private final OnlineUserService onlineUserService;
    private final ChatBroadcastService broadcastService;
    private final MessageService messageService;
    private final AttendeeDataRepository attendeeDataRepository;
    private final ObjectMapper objectMapper;
    private final RateLimiterService rateLimiterService;
    private final AnnouncementService announcementService;
    private final BanGuard banGuard;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String providerUserId = resolveProviderUserId(session);
        if (providerUserId == null) {
            log.warn("[WS_REJECT] 未授權的 /ws/chat 連線嘗試, sessionId={}", session.getId());
            session.close();
            return;
        }
        // 即時查 DB 判斷封禁（D2）：banned 者拒絕連線，不加入 presence、不送任何廣播。
        if (banGuard.isBanned(providerUserId)) {
            log.warn("[WS_REJECT] 被封禁使用者嘗試連線 /ws/chat, userId={}, sessionId={}", providerUserId, session.getId());
            session.close(new CloseStatus(4403, "banned"));
            return;
        }
        session.getAttributes().put(ATTR_USER_ID, providerUserId);

        // D5：以 ConcurrentWebSocketSessionDecorator 包裝後再存入 presence map；序列化同一 session
        // 的並發送出、對慢客戶端按 sendTimeLimit/bufferSizeLimit 自動關閉而非阻塞廣播迴圈。
        // 將同一 decorator 實例存入 attributes，afterConnectionClosed 才能以同一實例 race-safe remove。
        WebSocketSession decorated = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
        session.getAttributes().put(ATTR_WS_DECORATED, decorated);

        onlineUserService.swap(providerUserId, decorated).ifPresent(oldSession -> {
            log.info("[WS_KICK_SWAP] 同 user 新連線踢掉舊連線, userId={}, oldSessionId={}, newSessionId={}",
                    providerUserId, oldSession.getId(), session.getId());
            broadcastService.sendTo(oldSession, new ChatEnvelope<>(ChatEventType.KICKED, System.currentTimeMillis(), null));
            try {
                oldSession.close(new CloseStatus(4271, "kicked"));
            } catch (IOException ex) {
                log.warn("failed to close old session id={} reason={}", oldSession.getId(), ex.getMessage());
            }
        });

        // 初始送出一律走 decorated（已在 map 內）：swap 後其他執行緒的 broadcastToAll 可能同時對同一
        // 底層 socket 寫，若這裡用 raw session 送會繞過序列化、與 decorator 路徑並發寫同一 socket。
        PresenceSnapshotPayload snapshot = new PresenceSnapshotPayload(new ArrayList<>(onlineUserService.getOnlineUserIds()));
        broadcastService.sendTo(decorated, new ChatEnvelope<>(ChatEventType.PRESENCE_SNAPSHOT, System.currentTimeMillis(), snapshot));
        // 晚到者補送目前公告（接於 presence snapshot 之後）；無公告則不送
        String announcement = announcementService.getCurrent();
        if (announcement != null) {
            broadcastService.sendTo(decorated, new ChatEnvelope<>(
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
        // 每次入站 frame（含 PING）更新 lastSeenAt，避免活躍 session 被 idle 排程誤回收（D6）。
        onlineUserService.touch(userId);

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
            case TYPING -> handleTyping(userId);
            default -> sendError(session, "unsupported_inbound_type", "event type not accepted inbound: " + type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            return;
        }
        // 關鍵正確性：map 內存的是 decorator 實例；以原始 session remove 會 identity 不符而失敗→製造幽靈。
        // 取回連線時存於 attributes 的同一 decorator 實例來 remove；未存（理論上不會）則退回原始 session。
        Object decorated = session.getAttributes().get(ATTR_WS_DECORATED);
        WebSocketSession toRemove = decorated instanceof WebSocketSession ws ? ws : session;
        onlineUserService.remove(userId, toRemove);
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
            // client 只送 replyToMessageId（可缺省）；作者/摘要/時間一律由 MessageService 即時解析衍生，
            // 不採信 client 傳入的 replyToFurName/replyToContentSnippet。
            String replyToMessageId = data.path("replyToMessageId").isMissingNode() || data.path("replyToMessageId").isNull()
                    ? null : data.path("replyToMessageId").asText();
            persistAndBroadcast(session, userId, () -> messageService.persistText(userId, content, imageUrls, replyToMessageId));
            return;
        }

        if (messageType == MessageType.STICKER) {
            String stickerImageUrl = data.path("stickerImageUrl").isMissingNode() || data.path("stickerImageUrl").isNull()
                    ? null : data.path("stickerImageUrl").asText("");
            if (stickerImageUrl == null || !stickerImageUrl.startsWith("/sticker/")) {
                sendError(session, "sticker_image_url_invalid_prefix", "sticker url must start with /sticker/");
                return;
            }
            // 貼圖亦可「回覆某則訊息」（貼圖只是不能被拿來當回覆內容摘要，被回覆完全支援）；
            // 規則與 TEXT 分支一致：client 只送 replyToMessageId，其餘一律即時解析。
            String replyToMessageId = data.path("replyToMessageId").isMissingNode() || data.path("replyToMessageId").isNull()
                    ? null : data.path("replyToMessageId").asText();
            persistAndBroadcast(session, userId, () -> messageService.persistSticker(userId, stickerImageUrl, replyToMessageId));
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
        // 回覆預覽即時解析（非快照）：即使 persistText 只原樣存了 replyToMessageId，
        // 廣播時仍即時查詢目標訊息 + 作者，確保剛送出就反映當前狀態。
        ReplyPreview replyPreview = messageService.resolveReplyPreview(saved.getReplyToMessageId());
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
                saved.getCreatedDate(),
                saved.getReplyToMessageId(),
                replyPreview == null ? null : replyPreview.targetUserId(),
                replyPreview == null ? null : replyPreview.furName(),
                replyPreview == null ? null : replyPreview.contentSnippet(),
                replyPreview == null ? null : replyPreview.createdDate());
        broadcastService.broadcastToAll(new ChatEnvelope<>(ChatEventType.CHAT_MESSAGE, System.currentTimeMillis(), payload));
    }

    // TYPING 不是訊息：不消耗 rate-limiter token、不持久化，只查顯示欄位後廣播。
    // high-frequency 事件，log 採 debug 以免洗版。
    private void handleTyping(String userId) {
        AttendeeDataEntity attendee = attendeeDataRepository.findByUserId(userId).orElse(null);
        log.debug("[TYPING] 輸入中, userId={}", userId);
        TypingPayload payload = new TypingPayload(
                userId,
                attendee == null ? null : attendee.getFurName(),
                attendee == null ? null : attendee.getAvatar(),
                attendee == null ? null : attendee.getAvatarColor());
        broadcastService.broadcastToAll(new ChatEnvelope<>(ChatEventType.TYPING, System.currentTimeMillis(), payload));
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
