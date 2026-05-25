package com.sef.cli.chat.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

@Service
public class OnlineUserService {

    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void swap(String userId, WebSocketSession newSession, Consumer<WebSocketSession> oldSessionCallback) {
        sessions.compute(userId, (key, oldSession) -> {
            if (oldSession != null) {
                oldSessionCallback.accept(oldSession);
            }
            return newSession;
        });
    }

    public void remove(String userId, WebSocketSession session) {
        sessions.remove(userId, session);
    }

    public WebSocketSession getSession(String userId) {
        return sessions.get(userId);
    }

    public Set<String> getOnlineUserIds() {
        return Collections.unmodifiableSet(sessions.keySet());
    }

    public Collection<WebSocketSession> getAllSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }
}
