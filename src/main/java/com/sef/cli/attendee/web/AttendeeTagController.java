package com.sef.cli.attendee.web;

import com.sef.cli.api.AttendeeTagApi;
import com.sef.cli.api.request.AddTagRequest;
import com.sef.cli.api.request.RemoveTagRequest;
import com.sef.cli.api.response.TagResponse;
import com.sef.cli.attendee.service.AttendeeTagService;
import com.sef.cli.attendee.web.map.AttendeeDtoMapper;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.common.BanGuard;
import com.sef.cli.tag.entity.TagEntity;
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
public class AttendeeTagController implements AttendeeTagApi {

    private final AttendeeTagService attendeeTagService;
    private final AttendeeDtoMapper attendeeDtoMapper;
    private final BanGuard banGuard;

    @Override
    public ResponseEntity<ApiResponse<TagResponse>> addTag(AddTagRequest req) {
        String userId = currentUserId();
        banGuard.assertNotBanned(userId);
        TagEntity t = attendeeTagService.addTag(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(attendeeDtoMapper.toTagResponse(t)));
    }

    @Override
    public ResponseEntity<ApiResponse<Object>> removeTag(RemoveTagRequest req) {
        String userId = currentUserId();
        banGuard.assertNotBanned(userId);
        attendeeTagService.removeTag(userId, req.getTagId());
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
