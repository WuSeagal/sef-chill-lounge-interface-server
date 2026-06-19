package com.sef.cli.image.web;

import com.sef.cli.common.ApiResponse;
import com.sef.cli.common.BanGuard;
import com.sef.cli.image.service.ChatImageUploadService;
import com.sef.cli.image.web.dto.ChatImageUploadResponse;
import com.sef.cli.user.entity.AdminUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class ChatImageController {

    private final ChatImageUploadService uploadService;
    private final BanGuard banGuard;

    @PostMapping("/chat-image")
    public ResponseEntity<ApiResponse<ChatImageUploadResponse>> upload(
            @RequestParam("file") MultipartFile file) {
        String userId = currentUserId();
        banGuard.assertNotBanned(userId);
        ChatImageUploadResponse response = uploadService.upload(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AdminUserEntity user)) {
            throw new InsufficientAuthenticationException("no user");
        }
        return user.getProviderUserId();
    }
}
