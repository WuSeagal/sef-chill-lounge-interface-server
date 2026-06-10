package com.sef.cli.image.service;

import com.sef.cli.image.properties.ImageStorageProperties;
import com.sef.cli.image.web.dto.AvatarUploadResponse;
import com.sef.cli.image.web.exception.PayloadTooLargeException;
import com.sef.cli.image.web.exception.UnsupportedMediaTypeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarUploadService {

    private final ImageStorageProperties properties;

    public AvatarUploadResponse upload(MultipartFile file, String userId) {
        long maxBytes = (long) properties.getMaxFileSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            log.warn("[AVATAR_UPLOAD_FAIL] 檔案過大, userId={}, size={}, maxMb={}",
                    userId, file.getSize(), properties.getMaxFileSizeMb());
            throw new PayloadTooLargeException("file_too_large", properties.getMaxFileSizeMb());
        }

        String ext = extractExtension(file.getOriginalFilename());
        ImageFormat byExt = ImageFormat.matchExtension(ext);
        if (byExt == null) {
            log.warn("[AVATAR_UPLOAD_FAIL] 副檔名不支援, userId={}, reason=unsupported_extension, ext={}", userId, ext);
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }

        if (!byExt.matchesMime(file.getContentType())) {
            log.warn("[AVATAR_UPLOAD_FAIL] contentType 不符, userId={}, reason=mime_mismatch, contentType={}",
                    userId, file.getContentType());
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.warn("[AVATAR_UPLOAD_FAIL] 讀取檔案內容失敗, userId={}, reason=read_failed", userId);
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }

        byte[] header = Arrays.copyOf(bytes, 12);
        ImageFormat byMagic = MagicByteValidator.detectFormat(header);
        if (byMagic == null || byMagic != byExt) {
            log.warn("[AVATAR_UPLOAD_FAIL] magic byte 不符, userId={}, reason=magic_mismatch, ext={}", userId, ext);
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }

        deleteExistingAvatarFiles(userId);

        Path userDir = Paths.get(properties.getBasePath(), "user");
        Path dest = userDir.resolve(userId + "." + byMagic.normalizedExtension());
        try {
            Files.createDirectories(userDir);
            Files.write(dest, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write avatar", e);
        }

        long version;
        try {
            version = Files.getLastModifiedTime(dest).toMillis();
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve avatar version", e);
        }

        log.info("[AVATAR_UPLOAD] 頭像上傳成功, userId={}, fileName={}, size={}",
                userId, userId + "." + byMagic.normalizedExtension(), file.getSize());
        return new AvatarUploadResponse(
                properties.getUser().getUrlPrefix() + userId + "." + byMagic.normalizedExtension() + "?v=" + version);
    }

    private void deleteExistingAvatarFiles(String userId) {
        Path userDir = Paths.get(properties.getBasePath(), "user");
        for (ImageFormat format : ImageFormat.values()) {
            Path existing = userDir.resolve(userId + "." + format.normalizedExtension());
            try {
                Files.deleteIfExists(existing);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete previous avatar", e);
            }
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
