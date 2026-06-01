package com.sef.cli.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DashboardViewerServiceTest {

    private DashboardViewerService service;

    @BeforeEach
    void setUp() {
        service = new DashboardViewerService();
    }

    @Test
    void registerAddsSession() {
        WebSocketSession s = mock(WebSocketSession.class);
        service.register(s);
        assertThat(service.getAllSessions()).containsExactlyInAnyOrder(s);
    }

    @Test
    void multipleSessionsCoexist_noUniquenessByUser() {
        WebSocketSession s1 = mock(WebSocketSession.class);
        WebSocketSession s2 = mock(WebSocketSession.class);
        service.register(s1);
        service.register(s2);
        assertThat(service.getAllSessions()).containsExactlyInAnyOrder(s1, s2);
    }

    @Test
    void unregisterRemovesSession() {
        WebSocketSession s = mock(WebSocketSession.class);
        service.register(s);
        service.unregister(s);
        assertThat(service.getAllSessions()).isEmpty();
    }
}
