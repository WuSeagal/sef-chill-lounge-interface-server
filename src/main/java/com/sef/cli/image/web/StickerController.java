package com.sef.cli.image.web;

import com.sef.cli.api.response.StickerResponse;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.common.BanGuard;
import com.sef.cli.image.service.StickerUploadService;
import com.sef.cli.user.entity.AdminUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload/sticker")
@RequiredArgsConstructor
public class StickerController {

    private final StickerUploadService stickerUploadService;
    private final BanGuard banGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<StickerResponse>> uploadSticker(
            @RequestParam("file") MultipartFile file) {
        String userId = currentUserId();
        banGuard.assertNotBanned(userId);
        StickerResponse response = stickerUploadService.upload(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSticker(@PathVariable Long id) {
        String userId = currentUserId();
        banGuard.assertNotBanned(userId);
        stickerUploadService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.<Void>success(null));
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AdminUserEntity user)) {
            throw new InsufficientAuthenticationException("no user");
        }
        return user.getProviderUserId();
    }
}
