package com.sef.cli.image.service;

import com.sef.cli.image.entity.ChatImageAssetEntity;
import com.sef.cli.image.properties.ImageStorageProperties;
import com.sef.cli.image.repository.ChatImageAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatImageRetentionServiceTest {

    @Mock
    private ChatImageAssetRepository repository;

    @TempDir
    Path tempDir;

    private ChatImageRetentionService service;
    private ImageStorageProperties props;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(tempDir.resolve("image"));
        props = new ImageStorageProperties();
        props.setBasePath(tempDir.toString() + "/");
        props.getChat().setMaxCount(1000);
        props.getChat().setTtlDays(90);
        service = new ChatImageRetentionService(repository, props);
    }

    @Test
    void evictOldestNothingWhenUnderLimit() {
        when(repository.count()).thenReturn(500L);
        service.evictOldestIfOver();
        verify(repository).count();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void evictOldestRemovesExcessRowsAndFiles() throws Exception {
        Path fileA = Files.write(tempDir.resolve("image/old1.jpg"), "data".getBytes());
        Path fileB = Files.write(tempDir.resolve("image/old2.jpg"), "data".getBytes());

        when(repository.count()).thenReturn(1002L);
        when(repository.findOldestN(2)).thenReturn(List.of(
                ChatImageAssetEntity.builder().id(1L).fileName("old1.jpg").build(),
                ChatImageAssetEntity.builder().id(2L).fileName("old2.jpg").build()
        ));

        service.evictOldestIfOver();

        verify(repository).deleteAll(ArgumentMatchers.anyIterable());
        assertThat(Files.exists(fileA)).isFalse();
        assertThat(Files.exists(fileB)).isFalse();
    }

    @Test
    void dailyCleanupRemovesStaleRowsAndFiles() throws Exception {
        Path stale = Files.write(tempDir.resolve("image/stale.jpg"), "data".getBytes());

        when(repository.findByUploadedDateBefore(ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(ChatImageAssetEntity.builder()
                        .id(1L).fileName("stale.jpg").build()));
        when(repository.findAllFileNames()).thenReturn(List.of());

        service.dailyCleanup();

        verify(repository).deleteAll(ArgumentMatchers.anyIterable());
        assertThat(Files.exists(stale)).isFalse();
    }

    @Test
    void dailyCleanupRemovesOrphanFilesWithNoDbRow() throws Exception {
        Path orphan = Files.write(tempDir.resolve("image/orphan.jpg"), "data".getBytes());

        when(repository.findByUploadedDateBefore(ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(repository.findAllFileNames()).thenReturn(List.of());

        service.dailyCleanup();

        assertThat(Files.exists(orphan)).isFalse();
    }

    @Test
    void dailyCleanupKeepsKnownFiles() throws Exception {
        Path known = Files.write(tempDir.resolve("image/known.jpg"), "data".getBytes());

        when(repository.findByUploadedDateBefore(ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(repository.findAllFileNames()).thenReturn(List.of("known.jpg"));

        service.dailyCleanup();

        assertThat(Files.exists(known)).isTrue();
    }
}
