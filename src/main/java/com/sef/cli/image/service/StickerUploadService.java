package com.sef.cli.image.service;

import com.sef.cli.api.response.StickerResponse;
import com.sef.cli.attendee.entity.AttendeeStickerEntity;
import com.sef.cli.attendee.repository.AttendeeStickerRepository;
import com.sef.cli.image.properties.ImageStorageProperties;
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
public class StickerUploadService {

    private static final int MIN_SLOT = 1;
    private static final int MAX_SLOT = 5;

    private final ImageStorageProperties properties;
    private final AttendeeStickerRepository stickerRepository;

    public StickerResponse upload(MultipartFile file, String userId, int slot) {
        if (slot < MIN_SLOT || slot > MAX_SLOT) {
            throw new IllegalArgumentException("sticker_slot_out_of_range");
        }

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

        Path userDir = Paths.get(properties.getBasePath(), "sticker", userId);
        deleteExistingSlotFiles(userDir, slot);
        Path dest = userDir.resolve(slot + "." + byMagic.normalizedExtension());
        long version;
        try {
            Files.createDirectories(userDir);
            Files.write(dest, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            version = Files.getLastModifiedTime(dest).toMillis();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write sticker", e);
        }

        String url = properties.getSticker().getUrlPrefix() + userId + "/" + slot + "."
                + byMagic.normalizedExtension() + "?v=" + version;

        AttendeeStickerEntity entity = stickerRepository.findByUserIdAndStickerNo(userId, slot)
                .orElseGet(() -> AttendeeStickerEntity.builder().userId(userId).stickerNo(slot).build());
        entity.setSticker(url);
        AttendeeStickerEntity saved = stickerRepository.save(entity);

        return StickerResponse.builder()
                .id(saved.getId())
                .stickerNo(saved.getStickerNo())
                .sticker(saved.getSticker())
                .build();
    }

    private void deleteExistingSlotFiles(Path userDir, int slot) {
        for (ImageFormat format : ImageFormat.values()) {
            Path existing = userDir.resolve(slot + "." + format.normalizedExtension());
            try {
                Files.deleteIfExists(existing);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete previous sticker", e);
            }
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
