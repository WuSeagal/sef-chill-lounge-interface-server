package com.sef.cli.chat.config;

import com.sef.cli.chat.handler.ChatWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    @Test
    void registersChatHandlerAtSlashWsChat() {
        ChatWebSocketHandler handler = mock(ChatWebSocketHandler.class);
        WebSocketConfig config = new WebSocketConfig(handler);

        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);
        when(registry.addHandler(eq(handler), eq("/ws/chat"))).thenReturn(registration);
        when(registration.setAllowedOriginPatterns(Mockito.any())).thenReturn(registration);

        config.registerWebSocketHandlers(registry);

        verify(registry).addHandler(handler, "/ws/chat");
        verify(registration).setAllowedOriginPatterns(Mockito.any());
    }
}
