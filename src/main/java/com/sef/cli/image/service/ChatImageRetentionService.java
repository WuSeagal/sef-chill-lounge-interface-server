package com.sef.cli.image.service;

import com.sef.cli.image.entity.ChatImageAssetEntity;
import com.sef.cli.image.properties.ImageStorageProperties;
import com.sef.cli.image.repository.ChatImageAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatImageRetentionService {

    private final ChatImageAssetRepository repository;
    private final ImageStorageProperties properties;

    public void evictOldestIfOver() {
        long count = repository.count();
        int maxCount = properties.getChat().getMaxCount();
        if (count <= maxCount) return;

        int excess = (int) (count - maxCount);
        List<ChatImageAssetEntity> oldest = repository.findOldestN(excess);
        deleteFilesAndRows(oldest);
    }

    @Scheduled(cron = "${sef-images.chat.cleanup-cron:0 0 3 * * *}", zone = "Asia/Taipei")
    public void dailyCleanup() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(properties.getChat().getTtlDays());
        List<ChatImageAssetEntity> stale = repository.findByUploadedDateBefore(threshold);
        if (!stale.isEmpty()) {
            deleteFilesAndRows(stale);
            log.info("TTL cleanup removed {} stale image(s)", stale.size());
        }

        Set<String> knownNames = new HashSet<>();
        repository.findAll().forEach(e -> knownNames.add(e.getFileName()));

        Path dir = Paths.get(properties.getBasePath(), "image");
        if (!Files.exists(dir)) return;
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> !knownNames.contains(p.getFileName().toString()))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            log.info("Removed orphan image file: {}", p.getFileName());
                        } catch (IOException e) {
                            log.warn("Failed to delete orphan {}: {}", p, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to scan image directory {}: {}", dir, e.getMessage());
        }
    }

    private void deleteFilesAndRows(List<ChatImageAssetEntity> entities) {
        for (ChatImageAssetEntity e : entities) {
            Path file = Paths.get(properties.getBasePath(), "image", e.getFileName());
            try {
                Files.deleteIfExists(file);
            } catch (IOException ex) {
                log.warn("Failed to delete file {}: {}", file, ex.getMessage());
            }
        }
        repository.deleteAll(entities);
    }
}
