package com.sef.cli.chat.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OnlineUserService {

    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * Atomically install {@code newSession} as the active session for {@code userId}.
     * If a previous session existed, it is removed from the map and returned so the
     * caller can kick / close it OUTSIDE this map's bin lock. Keeping I/O work
     * (sendMessage / close) out of the compute callback prevents the close frame
     * from being swallowed and avoids any lock-coupling with the WebSocket runtime.
     */
    public Optional<WebSocketSession> swap(String userId, WebSocketSession newSession) {
        AtomicReference<WebSocketSession> displaced = new AtomicReference<>();
        sessions.compute(userId, (key, oldSession) -> {
            displaced.set(oldSession);
            return newSession;
        });
        return Optional.ofNullable(displaced.get());
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
