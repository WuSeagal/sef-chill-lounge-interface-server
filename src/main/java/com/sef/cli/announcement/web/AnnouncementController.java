package com.sef.cli.announcement.web;

import com.sef.cli.announcement.service.AnnouncementService;
import com.sef.cli.api.AnnouncementApi;
import com.sef.cli.api.request.AnnouncementRequest;
import com.sef.cli.chat.event.ChatEnvelope;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.event.response.AnnouncementPayload;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.common.HostAuthz;
import com.sef.cli.common.exception.ForbiddenException;
import com.sef.cli.user.entity.AdminUserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AnnouncementController implements AnnouncementApi {

    private static final int MAX_LEN = 200;

    private final AnnouncementService announcementService;
    private final ChatBroadcastService chatBroadcastService;

    @Override
    public ResponseEntity<ApiResponse<Void>> setAnnouncement(AnnouncementRequest req) {
        String requesterId = currentUserId();
        if (!HostAuthz.isHost(requesterId)) {
            throw new ForbiddenException();
        }

        String text = req.getText() == null ? "" : req.getText().trim();
        if (text.length() > MAX_LEN) {
            throw new IllegalArgumentException("announcement_too_long");
        }

        boolean pinned = !text.isEmpty();
        String value = pinned ? text : null;
        announcementService.set(value);
        // 變更（設定或清除）皆廣播；text=null 表示清除。dashboard sessions 也會收到但不消費。
        chatBroadcastService.broadcastToAll(new ChatEnvelope<>(
                ChatEventType.ANNOUNCEMENT,
                System.currentTimeMillis(),
                new AnnouncementPayload(value)
        ));
        log.info("[ANNOUNCEMENT] host={} action={} len={}", requesterId, pinned ? "pin" : "unpin", text.length());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AdminUserEntity user)) {
            throw new InsufficientAuthenticationException("no user");
        }
        return user.getProviderUserId();
    }
}
