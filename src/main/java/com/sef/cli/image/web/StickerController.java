package com.sef.cli.image.web;

import com.sef.cli.api.request.RemoveStickerRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    // api-delete-to-post：依專案 GET/POST-only 慣例，移除改走 POST /upload/sticker/remove + @RequestBody。
    @PostMapping("/remove")
    public ResponseEntity<ApiResponse<Void>> deleteSticker(@RequestBody RemoveStickerRequest request) {
        String userId = currentUserId();
        banGuard.assertNotBanned(userId);
        stickerUploadService.delete(request.getId(), userId);
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
