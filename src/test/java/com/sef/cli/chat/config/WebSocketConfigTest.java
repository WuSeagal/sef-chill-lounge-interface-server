package com.sef.cli.chat.config;

import com.sef.cli.chat.handler.ChatWebSocketHandler;
import com.sef.cli.chat.handler.DashboardWebSocketHandler;
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
        DashboardWebSocketHandler dashboardHandler = mock(DashboardWebSocketHandler.class);
        WebSocketConfig config = new WebSocketConfig(handler, dashboardHandler);

        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);
        WebSocketHandlerRegistration dashReg = mock(WebSocketHandlerRegistration.class);
        when(registry.addHandler(eq(handler), eq("/ws/chat"))).thenReturn(registration);
        when(registration.setAllowedOriginPatterns(Mockito.any())).thenReturn(registration);
        when(registry.addHandler(eq(dashboardHandler), eq("/ws/dashboard"))).thenReturn(dashReg);
        when(dashReg.setAllowedOriginPatterns(Mockito.any())).thenReturn(dashReg);

        config.registerWebSocketHandlers(registry);

        verify(registry).addHandler(handler, "/ws/chat");
        verify(registration).setAllowedOriginPatterns(Mockito.any());
    }

    @Test
    void registersDashboardHandlerAtSlashWsDashboard() {
        ChatWebSocketHandler chatHandler = mock(ChatWebSocketHandler.class);
        DashboardWebSocketHandler dashboardHandler = mock(DashboardWebSocketHandler.class);
        WebSocketConfig config = new WebSocketConfig(chatHandler, dashboardHandler);

        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration chatReg = mock(WebSocketHandlerRegistration.class);
        WebSocketHandlerRegistration dashReg = mock(WebSocketHandlerRegistration.class);
        when(registry.addHandler(eq(chatHandler), eq("/ws/chat"))).thenReturn(chatReg);
        when(chatReg.setAllowedOriginPatterns(Mockito.any())).thenReturn(chatReg);
        when(registry.addHandler(eq(dashboardHandler), eq("/ws/dashboard"))).thenReturn(dashReg);
        when(dashReg.setAllowedOriginPatterns(Mockito.any())).thenReturn(dashReg);

        config.registerWebSocketHandlers(registry);

        verify(registry).addHandler(dashboardHandler, "/ws/dashboard");
        verify(dashReg).setAllowedOriginPatterns(Mockito.any());
    }
}
