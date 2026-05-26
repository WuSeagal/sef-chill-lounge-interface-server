package com.sef.cli.image.config;

import com.sef.cli.image.properties.ImageStorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageDirectoriesInitializer {

    private final ImageStorageProperties properties;

    @PostConstruct
    public void ensureDirectories() {
        String base = properties.getBasePath();
        if (base == null || base.isBlank()) {
            log.warn("sef-images.base-path not configured; skipping directory init");
            return;
        }
        for (String sub : new String[]{"image", "user", "sticker"}) {
            Path p = Paths.get(base, sub);
            if (!Files.exists(p)) {
                try {
                    Files.createDirectories(p);
                    log.info("Created image subdirectory: {}", p);
                } catch (Exception e) {
                    log.warn("Failed to create subdirectory {}: {}", p, e.getMessage());
                }
            }
        }
    }
}
