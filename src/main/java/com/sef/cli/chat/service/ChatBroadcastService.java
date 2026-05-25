package com.sef.cli.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sef.cli.chat.event.ChatEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBroadcastService {

    private final OnlineUserService onlineUserService;
    private final ObjectMapper objectMapper;

    public void sendTo(WebSocketSession session, ChatEnvelope<?> envelope) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(envelope);
            session.sendMessage(new TextMessage(json));
        } catch (IOException ex) {
            log.warn("ws send failed sessionId={} type={} reason={}",
                    session.getId(), envelope.type(), ex.getMessage());
        }
    }

    public void broadcastToAll(ChatEnvelope<?> envelope) {
        for (WebSocketSession session : onlineUserService.getAllSessions()) {
            sendTo(session, envelope);
        }
    }
}
