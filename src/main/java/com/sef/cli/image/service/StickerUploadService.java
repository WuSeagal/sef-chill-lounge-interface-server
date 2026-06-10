package com.sef.cli.image.service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.sef.cli.api.response.StickerResponse;
import com.sef.cli.attendee.entity.AttendeeStickerEntity;
import com.sef.cli.attendee.repository.AttendeeStickerRepository;
import com.sef.cli.image.properties.ImageStorageProperties;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StickerUploadService {

    /** Max stickers a user can keep selectable (shown in picker). */
    private static final int MAX_ACTIVE = 5;
    /** Max physical files retained per user on disk (FIFO, protecting active). */
    private static final int MAX_FILES = 15;

    private static final char[] NANOID_ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

    private final ImageStorageProperties properties;
    private final AttendeeStickerRepository stickerRepository;

    public StickerResponse upload(MultipartFile file, String userId) {
        if (stickerRepository.countByUserId(userId) >= MAX_ACTIVE) {
            log.warn("[STICKER_UPLOAD_FAIL] 貼圖數量已達上限, userId={}, reason=sticker_limit_reached, max={}",
                    userId, MAX_ACTIVE);
            throw new IllegalArgumentException("sticker_limit_reached");
        }

        long maxBytes = (long) properties.getMaxFileSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            log.warn("[STICKER_UPLOAD_FAIL] 檔案過大, userId={}, size={}, maxMb={}",
                    userId, file.getSize(), properties.getMaxFileSizeMb());
            throw new PayloadTooLargeException("file_too_large", properties.getMaxFileSizeMb());
        }

        String ext = extractExtension(file.getOriginalFilename());
        ImageFormat byExt = ImageFormat.matchExtension(ext);
        if (byExt == null) {
            log.warn("[STICKER_UPLOAD_FAIL] 副檔名不支援, userId={}, reason=unsupported_extension, ext={}", userId, ext);
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }
        if (!byExt.matchesMime(file.getContentType())) {
            log.warn("[STICKER_UPLOAD_FAIL] contentType 不符, userId={}, reason=mime_mismatch, contentType={}",
                    userId, file.getContentType());
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.warn("[STICKER_UPLOAD_FAIL] 讀取檔案內容失敗, userId={}, reason=read_failed", userId);
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }
        byte[] header = Arrays.copyOf(bytes, 12);
        ImageFormat byMagic = MagicByteValidator.detectFormat(header);
        if (byMagic == null || byMagic != byExt) {
            log.warn("[STICKER_UPLOAD_FAIL] magic byte 不符, userId={}, reason=magic_mismatch, ext={}", userId, ext);
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }

        // Filename mirrors chat-image scheme: {userSuffix}-{ts}-{nano}.{ext}.
        // ts (fixed-width yyMMddHHmmss) + same per-user dir means lexical sort == chronological.
        String userSuffix = userId.length() <= 6 ? userId : userId.substring(userId.length() - 6);
        String ts = LocalDateTime.now(TAIPEI).format(TS_FORMATTER);
        String nano3 = NanoIdUtils.randomNanoId(RANDOM, NANOID_ALPHABET, 3);
        String fileName = userSuffix + "-" + ts + "-" + nano3 + "." + byMagic.normalizedExtension();

        Path userDir = Paths.get(properties.getBasePath(), "sticker", userId);
        Path dest = userDir.resolve(fileName);
        try {
            Files.createDirectories(userDir);
            Files.write(dest, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write sticker", e);
        }

        String url = properties.getSticker().getUrlPrefix() + userId + "/" + fileName;
        AttendeeStickerEntity saved = stickerRepository.save(
                AttendeeStickerEntity.builder().userId(userId).sticker(url).build());

        evictOldestNonActiveBeyondCap(userId);

        log.info("[STICKER_UPLOAD] 貼圖上傳成功, userId={}, id={}, fileName={}, size={}",
                userId, saved.getId(), fileName, file.getSize());
        return StickerResponse.builder().id(saved.getId()).sticker(saved.getSticker()).build();
    }

    public void delete(Long id, String userId) {
        AttendeeStickerEntity row = stickerRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> {
                    log.warn("[STICKER_DELETE_FAIL] 貼圖不存在或無權限, userId={}, id={}, reason=sticker_not_found",
                            userId, id);
                    return new IllegalArgumentException("sticker_not_found");
                });
        stickerRepository.delete(row);
        log.info("[STICKER_DELETE] 貼圖刪除成功, userId={}, id={}", userId, id);
        // File intentionally NOT deleted here: already-sent chat messages still
        // reference it. The 15-file FIFO (evictOldestNonActiveBeyondCap) reclaims it later.
    }

    /**
     * Keeps at most {@link #MAX_FILES} files per user on disk. Deletes the oldest files
     * (lexical filename order == chronological) that are NOT referenced by any active
     * sticker row, so a sticker the user is currently using is never evicted.
     */
    private void evictOldestNonActiveBeyondCap(String userId) {
        Path userDir = Paths.get(properties.getBasePath(), "sticker", userId);
        if (!Files.exists(userDir)) return;

        List<Path> files;
        try (Stream<Path> s = Files.list(userDir)) {
            files = s.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return;
        }
        int excess = files.size() - MAX_FILES;
        if (excess <= 0) return;

        Set<String> activeNames = stickerRepository.findByUserId(userId).stream()
                .map(e -> fileNameFromUrl(e.getSticker()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        int deleted = 0;
        for (Path p : files) {
            if (deleted >= excess) break;
            if (activeNames.contains(p.getFileName().toString())) continue;
            try {
                Files.deleteIfExists(p);
                deleted++;
            } catch (IOException ignored) {
                // best-effort; orphan stays until next eviction
            }
        }
    }

    private String fileNameFromUrl(String url) {
        if (url == null) return null;
        int q = url.indexOf('?');
        String u = q >= 0 ? url.substring(0, q) : url;
        int slash = u.lastIndexOf('/');
        return slash >= 0 ? u.substring(slash + 1) : u;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
