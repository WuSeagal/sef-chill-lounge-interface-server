package com.sef.cli.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OnlineUserServiceTest {

    private OnlineUserService service;

    @BeforeEach
    void setUp() {
        service = new OnlineUserService();
    }

    @Test
    void swapPutsNewSessionAndExposesNoOldSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        AtomicReference<WebSocketSession> seenOld = new AtomicReference<>();

        service.swap("u-1", session, seenOld::set);

        assertThat(seenOld.get()).isNull();
        assertThat(service.getOnlineUserIds()).containsExactly("u-1");
    }

    @Test
    void swapInvokesCallbackWithOldSessionWhenReplacing() {
        WebSocketSession oldSession = mock(WebSocketSession.class);
        WebSocketSession newSession = mock(WebSocketSession.class);
        service.swap("u-1", oldSession, ignored -> {});

        AtomicReference<WebSocketSession> seenOld = new AtomicReference<>();
        service.swap("u-1", newSession, seenOld::set);

        assertThat(seenOld.get()).isSameAs(oldSession);
        assertThat(service.getSession("u-1")).isSameAs(newSession);
    }

    @Test
    void removeOnlyHappensWhenSessionMatches() {
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);
        service.swap("u-1", sessionA, ignored -> {});
        service.swap("u-1", sessionB, ignored -> {});

        service.remove("u-1", sessionA);

        assertThat(service.getSession("u-1")).isSameAs(sessionB);
    }

    @Test
    void removeClearsEntryWhenSessionMatches() {
        WebSocketSession session = mock(WebSocketSession.class);
        service.swap("u-1", session, ignored -> {});

        service.remove("u-1", session);

        assertThat(service.getOnlineUserIds()).isEmpty();
    }

    @Test
    void getOnlineUserIdsReturnsImmutableSnapshot() {
        WebSocketSession session = mock(WebSocketSession.class);
        service.swap("u-1", session, ignored -> {});

        assertThat(service.getOnlineUserIds()).isUnmodifiable();
    }
}
