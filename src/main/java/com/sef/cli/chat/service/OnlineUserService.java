package com.sef.cli.chat.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

@Service
public class OnlineUserService {

    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> lastSeenAt = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public OnlineUserService() {
        this(System::currentTimeMillis);
    }

    // 可注入 clock 以利測試（對齊 RateLimiterService 慣例），避免直接呼叫 System.currentTimeMillis 難測。
    OnlineUserService(LongSupplier clock) {
        this.clock = clock;
    }

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
        // 連線建立即記錄 lastSeenAt，供 idle 回收門檻使用（D6）。
        lastSeenAt.put(userId, clock.getAsLong());
        return Optional.ofNullable(displaced.get());
    }

    public void remove(String userId, WebSocketSession session) {
        // 僅當 map 內當前值為該 session 時才移除（race-safe）；移除成功才清 lastSeenAt，
        // 否則新 swap 進來的 session 會誤失其時間戳。
        if (sessions.remove(userId, session)) {
            lastSeenAt.remove(userId);
        }
    }

    /** 每次收到該 user 的入站 frame（含 PING）時更新 lastSeenAt（D6）。 */
    public void touch(String userId) {
        // 僅在該 user 仍在線時更新，避免為已移除者留下殘留時間戳。
        if (sessions.containsKey(userId)) {
            lastSeenAt.put(userId, clock.getAsLong());
        }
    }

    /**
     * 掃描並移除幽靈/閒置 session：{@code !isOpen()} 或距 {@code lastSeenAt} 超過
     * {@code idleMs} 者。回傳被移除的 userId 清單（供呼叫端重廣播 PRESENCE_SNAPSHOT）。
     */
    public List<String> reapStale(long idleMs) {
        long now = clock.getAsLong();
        List<String> removed = new ArrayList<>();
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String userId = entry.getKey();
            WebSocketSession session = entry.getValue();
            Long seen = lastSeenAt.get(userId);
            boolean closed = session == null || !session.isOpen();
            boolean idle = seen != null && (now - seen) > idleMs;
            if (closed || idle) {
                remove(userId, session);
                removed.add(userId);
            }
        }
        return removed;
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
