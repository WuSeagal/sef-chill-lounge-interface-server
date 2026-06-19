package com.sef.cli.feedback.web;

import com.sef.cli.api.FeedbackApi;
import com.sef.cli.api.request.FeedbackRequest;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.common.BanGuard;
import com.sef.cli.feedback.service.FeedbackMailService;
import com.sef.cli.user.entity.AdminUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FeedbackController implements FeedbackApi {

    private final FeedbackMailService feedbackMailService;
    private final BanGuard banGuard;

    @Override
    public ResponseEntity<ApiResponse<Void>> submit(FeedbackRequest request) {
        // 縱深防禦：被封禁者不可送出 feedback（封禁畫面導向外部 email，非此 in-app 表單）。
        banGuard.assertNotBanned(currentUserId());
        feedbackMailService.send(request);
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
