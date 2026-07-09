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
import com.sef.cli.common.BanGuard;
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
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
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
    private static final String ATTR_WS_DECORATED = "wsDecorated";
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024;

    private final DashboardViewerService viewerService;
    private final ChatBroadcastService broadcastService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final OnlineUserService onlineUserService;
    private final BanGuard banGuard;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String providerUserId = resolveProviderUserId(session);
        if (providerUserId == null) {
            log.warn("[DASH_REJECT] 未授權的 /ws/dashboard 連線嘗試, sessionId={}", session.getId());
            session.close();
            return;
        }
        // 即時查 DB 判斷封禁（D2）：banned 者拒絕連線，不加入 viewer registry。
        if (banGuard.isBanned(providerUserId)) {
            log.warn("[DASH_REJECT] 被封禁使用者嘗試連線 /ws/dashboard, userId={}, sessionId={}", providerUserId, session.getId());
            session.close(new CloseStatus(4403, "banned"));
            return;
        }
        // D5：以 ConcurrentWebSocketSessionDecorator 包裝後再註冊；序列化並發送出＋慢客戶端 backpressure。
        // 將同一 decorator 實例存入 attributes，afterConnectionClosed 才能以同一實例 unregister。
        WebSocketSession decorated = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
        session.getAttributes().put(ATTR_WS_DECORATED, decorated);

        // Register before replay so live messages arriving mid-replay are also delivered
        // (frontend dedups by messageId). On replay failure, undo the registration so a
        // dead viewer is not left behind. 註冊/註銷皆用同一 decorator 實例。
        viewerService.register(decorated);
        try {
            // 用 decorated 送 replay/snapshot：register 後其他執行緒的 broadcastToAll 可能同時對同一
            // socket 寫，raw session 送會繞過序列化。replay 期間（最多 30 筆）窗口較長更需如此。
            replayRecentHistory(decorated);
            sendPresenceSnapshot(decorated);
            log.info("[DASH_CONNECT] dashboard viewer 連線, sessionId={}, viewers={}",
                    session.getId(), viewerService.getAllSessions().size());
        } catch (RuntimeException ex) {
            viewerService.unregister(decorated);
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
        // viewer set 內存的是 decorator 實例；取回連線時存於 attributes 的同一實例 unregister，
        // 否則 identity 不符而移除失敗→殘留幽靈 viewer。未存（理論上不會）則退回原始 session。
        Object decorated = session.getAttributes().get(ATTR_WS_DECORATED);
        WebSocketSession toUnregister = decorated instanceof WebSocketSession ws ? ws : session;
        viewerService.unregister(toUnregister);
        log.info("[DASH_DISCONNECT] dashboard viewer 斷線, sessionId={}, viewers={}",
                session.getId(), viewerService.getAllSessions().size());
    }

    private void sendPresenceSnapshot(WebSocketSession target) {
        PresenceSnapshotPayload snapshot = new PresenceSnapshotPayload(new ArrayList<>(onlineUserService.getOnlineUserIds()));
        broadcastService.sendTo(target, new ChatEnvelope<>(ChatEventType.PRESENCE_SNAPSHOT, System.currentTimeMillis(), snapshot));
    }

    private void replayRecentHistory(WebSocketSession target) {
        List<MessageHistoryData> recent = messageService.loadHistory(null, null, REPLAY_LIMIT);
        // loadHistory returns newest-first; replay oldest-first so bubbles arrive in order
        for (int i = recent.size() - 1; i >= 0; i--) {
            MessageHistoryData d = recent.get(i);
            ChatMessageBroadcast payload = new ChatMessageBroadcast(
                    d.cursorId(), d.messageId(), d.userId(), d.furName(), d.avatar(),
                    d.avatarColor(), d.avatarBorder(), d.messageType(), d.content(),
                    d.imageUrls(), d.stickerImageUrl(), d.createdDate(),
                    d.replyToMessageId(), d.replyToUserId(), d.replyToFurName(),
                    d.replyToContentSnippet(), d.replyToCreatedDate());
            broadcastService.sendTo(target, new ChatEnvelope<>(ChatEventType.CHAT_MESSAGE, System.currentTimeMillis(), payload));
        }
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
