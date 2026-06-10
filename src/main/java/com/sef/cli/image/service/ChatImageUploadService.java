package com.sef.cli.image.service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.sef.cli.image.entity.ChatImageAssetEntity;
import com.sef.cli.image.properties.ImageStorageProperties;
import com.sef.cli.image.repository.ChatImageAssetRepository;
import com.sef.cli.image.web.dto.ChatImageUploadResponse;
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
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatImageUploadService {

    private static final char[] NANOID_ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

    private final ChatImageAssetRepository repository;
    private final ChatImageRetentionService retention;
    private final ImageStorageProperties properties;

    public ChatImageUploadResponse upload(MultipartFile file, String userId) {
        long maxBytes = (long) properties.getMaxFileSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            log.warn("[CHAT_IMAGE_UPLOAD_FAIL] 檔案過大, userId={}, size={}, maxMb={}",
                    userId, file.getSize(), properties.getMaxFileSizeMb());
            throw new PayloadTooLargeException("file_too_large", properties.getMaxFileSizeMb());
        }

        String originalName = file.getOriginalFilename();
        String ext = extractExtension(originalName);

        ImageFormat byExt = ImageFormat.matchExtension(ext);
        if (byExt == null) {
            log.warn("[CHAT_IMAGE_UPLOAD_FAIL] 副檔名不支援, userId={}, reason=unsupported_extension, ext={}", userId, ext);
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }

        if (!byExt.matchesMime(file.getContentType())) {
            log.warn("[CHAT_IMAGE_UPLOAD_FAIL] contentType 不符, userId={}, reason=mime_mismatch, contentType={}",
                    userId, file.getContentType());
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.warn("[CHAT_IMAGE_UPLOAD_FAIL] 讀取檔案內容失敗, userId={}, reason=read_failed", userId);
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }
        // Arrays.copyOf 不足 12 自動補 0；不必三元
        byte[] header = Arrays.copyOf(bytes, 12);
        ImageFormat byMagic = MagicByteValidator.detectFormat(header);
        if (byMagic == null || byMagic != byExt) {
            log.warn("[CHAT_IMAGE_UPLOAD_FAIL] magic byte 不符, userId={}, reason=magic_mismatch, ext={}", userId, ext);
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }

        String userSuffix = userId.length() <= 6 ? userId : userId.substring(userId.length() - 6);
        String ts = LocalDateTime.now(TAIPEI).format(TS_FORMATTER);
        String nano3 = NanoIdUtils.randomNanoId(RANDOM, NANOID_ALPHABET, 3);
        String fileName = userSuffix + "-" + ts + "-" + nano3 + "." + byMagic.normalizedExtension();

        Path dest = Paths.get(properties.getBasePath(), "image", fileName);
        try {
            // CREATE_NEW 拒絕覆蓋既有檔；若同秒同 user 撞 nano3 會 throw FileAlreadyExistsException
            Files.write(dest, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write image", e);
        }

        ChatImageAssetEntity entity = ChatImageAssetEntity.builder()
                .fileName(fileName)
                .uploadedBy(userId)
                .fileSize(file.getSize())
                .build();
        try {
            repository.save(entity);
        } catch (RuntimeException dbEx) {
            // DB 寫入失敗 → 主動刪剛寫好的檔避免孤兒（排程兜底是 last resort）
            try {
                Files.deleteIfExists(dest);
            } catch (IOException ignored) {
                // best-effort：排程 dailyCleanup 仍會掃到
            }
            throw dbEx;
        }

        retention.evictOldestIfOver();

        String url = properties.getChat().getUrlPrefix() + fileName;
        log.info("[CHAT_IMAGE_UPLOAD] 聊天圖片上傳成功, userId={}, fileName={}, size={}",
                userId, fileName, file.getSize());
        return new ChatImageUploadResponse(fileName, url);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
