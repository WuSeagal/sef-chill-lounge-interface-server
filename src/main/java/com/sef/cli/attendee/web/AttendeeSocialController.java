package com.sef.cli.attendee.web;

import com.sef.cli.api.AttendeeSocialApi;
import com.sef.cli.api.request.AddSocialLinkRequest;
import com.sef.cli.api.request.RemoveSocialLinkRequest;
import com.sef.cli.api.response.SocialResponse;
import com.sef.cli.attendee.entity.AttendeeSocialEntity;
import com.sef.cli.attendee.service.AttendeeSocialService;
import com.sef.cli.attendee.web.map.AttendeeDtoMapper;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.common.BanGuard;
import com.sef.cli.user.entity.AdminUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AttendeeSocialController implements AttendeeSocialApi {

    private final AttendeeSocialService service;
    private final AttendeeDtoMapper mapper;
    private final BanGuard banGuard;

    @Override
    public ResponseEntity<ApiResponse<SocialResponse>> addSocialLink(AddSocialLinkRequest req) {
        String userId = currentUserId();
        banGuard.assertNotBanned(userId);
        AttendeeSocialEntity e = service.addSocialLink(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(mapper.toSocialResponse(e)));
    }

    @Override
    public ResponseEntity<ApiResponse<Object>> removeSocialLink(RemoveSocialLinkRequest req) {
        String userId = currentUserId();
        banGuard.assertNotBanned(userId);
        service.removeSocialLink(userId, req.getId());
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
