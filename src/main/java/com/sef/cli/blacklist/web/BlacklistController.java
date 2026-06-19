package com.sef.cli.blacklist.web;

import com.sef.cli.api.BlacklistApi;
import com.sef.cli.api.request.BanRequest;
import com.sef.cli.api.response.BlacklistEntryResponse;
import com.sef.cli.blacklist.service.BlacklistService;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BlacklistController implements BlacklistApi {

    private final BlacklistService blacklistService;

    @Override
    public ResponseEntity<ApiResponse<List<BlacklistEntryResponse>>> getBlacklist() {
        requireHost();
        return ResponseEntity.ok(ApiResponse.success(blacklistService.list()));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> ban(BanRequest req) {
        String host = requireHost();
        String userId = req.getUserId();
        // 究極保護：host 不可被自己（或任何人）封禁，避免唯一特權帳號自我鎖死。
        if (HostAuthz.isHost(userId)) {
            log.warn("[BLACKLIST_BAN_FAIL] host={} userId={} reason=cannot_ban_host", host, userId);
            return ResponseEntity.ok(ApiResponse.fail(403, "cannot_ban_host"));
        }
        boolean ok = blacklistService.ban(userId);
        if (!ok) {
            log.warn("[BLACKLIST_BAN_FAIL] host={} userId={} reason=user_not_found", host, userId);
            return ResponseEntity.ok(ApiResponse.fail(404, "user_not_found"));
        }
        log.info("[BLACKLIST_BAN] host={} userId={}", host, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> unban(BanRequest req) {
        String host = requireHost();
        String userId = req.getUserId();
        blacklistService.unban(userId);
        log.info("[BLACKLIST_UNBAN] host={} userId={}", host, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private String requireHost() {
        String requesterId = currentUserId();
        if (!HostAuthz.isHost(requesterId)) {
            throw new ForbiddenException();
        }
        return requesterId;
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AdminUserEntity user)) {
            throw new InsufficientAuthenticationException("no user");
        }
        return user.getProviderUserId();
    }
}
