package com.sef.cli.image.service;

import com.sef.cli.image.properties.ImageStorageProperties;
import com.sef.cli.image.web.dto.AvatarUploadResponse;
import com.sef.cli.image.web.exception.PayloadTooLargeException;
import com.sef.cli.image.web.exception.UnsupportedMediaTypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AvatarUploadService {

    private final ImageStorageProperties properties;

    public AvatarUploadResponse upload(MultipartFile file, String userId) {
        long maxBytes = (long) properties.getMaxFileSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new PayloadTooLargeException("file_too_large", properties.getMaxFileSizeMb());
        }

        String ext = extractExtension(file.getOriginalFilename());
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

        byte[] header = Arrays.copyOf(bytes, 12);
        ImageFormat byMagic = MagicByteValidator.detectFormat(header);
        if (byMagic == null || byMagic != byExt) {
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
