package com.sef.cli.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OnlineUserServiceTest {

    private OnlineUserService service;

    @BeforeEach
    void setUp() {
        service = new OnlineUserService();
    }

    @Test
    void swapPutsNewSessionAndReturnsEmptyWhenNoPreviousSession() {
        WebSocketSession session = mock(WebSocketSession.class);

        Optional<WebSocketSession> displaced = service.swap("u-1", session);

        assertThat(displaced).isEmpty();
        assertThat(service.getOnlineUserIds()).containsExactly("u-1");
    }

    @Test
    void swapReturnsOldSessionWhenReplacing() {
        WebSocketSession oldSession = mock(WebSocketSession.class);
        WebSocketSession newSession = mock(WebSocketSession.class);
        service.swap("u-1", oldSession);

        Optional<WebSocketSession> displaced = service.swap("u-1", newSession);

        assertThat(displaced).contains(oldSession);
        assertThat(service.getSession("u-1")).isSameAs(newSession);
    }

    @Test
    void removeOnlyHappensWhenSessionMatches() {
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);
        service.swap("u-1", sessionA);
        service.swap("u-1", sessionB);

        service.remove("u-1", sessionA);

        assertThat(service.getSession("u-1")).isSameAs(sessionB);
    }

    @Test
    void removeClearsEntryWhenSessionMatches() {
        WebSocketSession session = mock(WebSocketSession.class);
        service.swap("u-1", session);

        service.remove("u-1", session);

        assertThat(service.getOnlineUserIds()).isEmpty();
    }

    @Test
    void getOnlineUserIdsReturnsImmutableSnapshot() {
        WebSocketSession session = mock(WebSocketSession.class);
        service.swap("u-1", session);

        assertThat(service.getOnlineUserIds()).isUnmodifiable();
    }

    // ---- idle/ghost presence 回收（D6）----

    private static final long IDLE_TIMEOUT_MS = 90_000L;

    @Test
    void swapRecordsLastSeenAtFromClock() {
        AtomicLong now = new AtomicLong(1_000L);
        OnlineUserService svc = new OnlineUserService(now::get);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        svc.swap("u-1", session);

        // 剛 swap，lastSeenAt=now，未逾時 → 不應被回收
        now.set(1_000L + IDLE_TIMEOUT_MS); // 剛好等於門檻，尚未「超過」
        assertThat(svc.reapStale(IDLE_TIMEOUT_MS)).isEmpty();
        assertThat(svc.getOnlineUserIds()).containsExactly("u-1");
    }

    @Test
    void reapRemovesClosedGhostSession() {
        AtomicLong now = new AtomicLong(0L);
        OnlineUserService svc = new OnlineUserService(now::get);
        WebSocketSession closed = mock(WebSocketSession.class);
        when(closed.isOpen()).thenReturn(false);
        svc.swap("ghost", closed);

        List<String> removed = svc.reapStale(IDLE_TIMEOUT_MS);

        assertThat(removed).containsExactly("ghost");
        assertThat(svc.getOnlineUserIds()).isEmpty();
    }

    @Test
    void reapRemovesIdleSessionPastTimeout() {
        AtomicLong now = new AtomicLong(0L);
        OnlineUserService svc = new OnlineUserService(now::get);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        svc.swap("idle", session); // lastSeenAt = 0

        now.set(IDLE_TIMEOUT_MS + 1); // 距 lastSeenAt 已超過門檻
        List<String> removed = svc.reapStale(IDLE_TIMEOUT_MS);

        assertThat(removed).containsExactly("idle");
        assertThat(svc.getOnlineUserIds()).isEmpty();
    }

    @Test
    void reapKeepsActiveSessionWithinTimeout() {
        AtomicLong now = new AtomicLong(0L);
        OnlineUserService svc = new OnlineUserService(now::get);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        svc.swap("active", session); // lastSeenAt = 0

        now.set(50_000L); // 未到門檻
        List<String> removed = svc.reapStale(IDLE_TIMEOUT_MS);

        assertThat(removed).isEmpty();
        assertThat(svc.getOnlineUserIds()).containsExactly("active");
    }

    @Test
    void touchUpdatesLastSeenAtSoSessionSurvivesReap() {
        AtomicLong now = new AtomicLong(0L);
        OnlineUserService svc = new OnlineUserService(now::get);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        svc.swap("u-1", session); // lastSeenAt = 0

        now.set(80_000L);
        svc.touch("u-1"); // lastSeenAt = 80_000

        now.set(80_000L + IDLE_TIMEOUT_MS); // 距上次 touch 剛好門檻，尚未超過
        assertThat(svc.reapStale(IDLE_TIMEOUT_MS)).isEmpty();
        assertThat(svc.getOnlineUserIds()).containsExactly("u-1");
    }

    @Test
    void removeAlsoClearsLastSeenAt() {
        AtomicLong now = new AtomicLong(0L);
        OnlineUserService svc = new OnlineUserService(now::get);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        svc.swap("u-1", session);

        svc.remove("u-1", session);

        // remove 後再次 swap 一個新 session，舊的 lastSeenAt 不得殘留影響新 session
        WebSocketSession fresh = mock(WebSocketSession.class);
        when(fresh.isOpen()).thenReturn(true);
        now.set(IDLE_TIMEOUT_MS * 2);
        svc.swap("u-1", fresh); // lastSeenAt 應為 now（新鮮）

        now.set(IDLE_TIMEOUT_MS * 2 + 10); // 距新 session 的 lastSeenAt 僅 10ms
        assertThat(svc.reapStale(IDLE_TIMEOUT_MS)).isEmpty();
        assertThat(svc.getOnlineUserIds()).containsExactly("u-1");
    }
}
