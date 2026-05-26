package com.sef.cli.image.service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.sef.cli.image.entity.ChatImageAssetEntity;
import com.sef.cli.image.properties.ImageStorageProperties;
import com.sef.cli.image.repository.ChatImageAssetRepository;
import com.sef.cli.image.web.dto.ChatImageUploadResponse;
import com.sef.cli.image.web.exception.PayloadTooLargeException;
import com.sef.cli.image.web.exception.UnsupportedMediaTypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

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
            throw new PayloadTooLargeException("file_too_large", properties.getMaxFileSizeMb());
        }

        String originalName = file.getOriginalFilename();
        String ext = extractExtension(originalName);

        ImageFormat byExt = ImageFormat.matchExtension(ext);
        if (byExt == null) {
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }

        if (!byExt.matchesMime(file.getContentType())) {
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }
        byte[] header = bytes.length >= 12 ? Arrays.copyOf(bytes, 12) : Arrays.copyOf(bytes, 12);
        ImageFormat byMagic = MagicByteValidator.detectFormat(header);
        if (byMagic == null || byMagic != byExt) {
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }

        String userSuffix = userId.length() <= 6 ? userId : userId.substring(userId.length() - 6);
        String ts = LocalDateTime.now(TAIPEI).format(TS_FORMATTER);
        String nano3 = NanoIdUtils.randomNanoId(RANDOM, NANOID_ALPHABET, 3);
        String fileName = userSuffix + "-" + ts + "-" + nano3 + "." + byMagic.normalizedExtension();

        Path dest = Paths.get(properties.getBasePath(), "image", fileName);
        try {
            Files.write(dest, bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write image", e);
        }

        ChatImageAssetEntity entity = ChatImageAssetEntity.builder()
                .fileName(fileName)
                .uploadedBy(userId)
                .fileSize(file.getSize())
                .build();
        repository.save(entity);

        retention.evictOldestIfOver();

        String url = properties.getChat().getUrlPrefix() + fileName;
        return new ChatImageUploadResponse(fileName, url);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
