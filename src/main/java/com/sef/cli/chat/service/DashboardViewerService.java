package com.sef.cli.chat.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds passive read-only dashboard viewer sessions. Unlike {@link OnlineUserService}
 * this has NO per-user uniqueness: many /dashboard connections (even same user) coexist
 * and never kick each other, and viewers are intentionally NOT part of presence.
 */
@Service
public class DashboardViewerService {

    private final Set<WebSocketSession> viewers = ConcurrentHashMap.newKeySet();

    public void register(WebSocketSession session) {
        viewers.add(session);
    }

    /** Removes the session from the viewer set; no-op if the session was never registered. */
    public void unregister(WebSocketSession session) {
        viewers.remove(session);
    }

    public Collection<WebSocketSession> getAllSessions() {
        return Collections.unmodifiableCollection(viewers);
    }
}
