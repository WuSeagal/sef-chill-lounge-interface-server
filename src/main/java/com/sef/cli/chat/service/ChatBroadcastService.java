package com.sef.cli.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sef.cli.chat.event.ChatEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBroadcastService {

    private final OnlineUserService onlineUserService;
    private final DashboardViewerService dashboardViewerService;
    private final ObjectMapper objectMapper;

    public void sendTo(WebSocketSession session, ChatEnvelope<?> envelope) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(envelope);
            session.sendMessage(new TextMessage(json));
        } catch (Exception ex) {
            // 放寬為 Exception（D5）：除 IOException 外，ConcurrentWebSocketSessionDecorator
            // 對慢客戶端可能擲 RuntimeException（如逾時/緩衝超限），單一收件者失敗不得中斷
            // broadcastToAll 整圈廣播，也不得逆流回觸發廣播的執行緒而斷掉發送者連線。
            log.warn("ws send failed sessionId={} type={} reason={}",
                    session.getId(), envelope.type(), ex.getMessage());
        }
    }

    public void broadcastToAll(ChatEnvelope<?> envelope) {
        for (WebSocketSession session : onlineUserService.getAllSessions()) {
            sendTo(session, envelope);
        }
        for (WebSocketSession session : dashboardViewerService.getAllSessions()) {
            sendTo(session, envelope);
        }
    }
}
