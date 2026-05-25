package com.sef.cli.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
}
