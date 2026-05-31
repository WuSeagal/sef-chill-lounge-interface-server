package com.sef.cli.image.service;

import com.sef.cli.api.response.StickerResponse;
import com.sef.cli.attendee.entity.AttendeeStickerEntity;
import com.sef.cli.attendee.repository.AttendeeStickerRepository;
import com.sef.cli.image.properties.ImageStorageProperties;
import com.sef.cli.image.web.exception.PayloadTooLargeException;
import com.sef.cli.image.web.exception.UnsupportedMediaTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StickerUploadServiceTest {

    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 0, 0, 0, 0
    };
    private static final byte[] VALID_GIF_BYTES = new byte[]{
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    @TempDir
    Path tempDir;

    private AttendeeStickerRepository repository;
    private StickerUploadService service;

    @BeforeEach
    void setUp() throws Exception {
        ImageStorageProperties props = new ImageStorageProperties();
        props.setBasePath(tempDir.toString() + "/");
        props.setMaxFileSizeMb(10);
        props.getSticker().setUrlPrefix("/sticker/");
        Files.createDirectories(tempDir.resolve("sticker"));
        repository = mock(AttendeeStickerRepository.class);
        service = new StickerUploadService(props, repository);
    }

    @Test
    void uploadStoresFileAndAddsRow() throws Exception {
        when(repository.countByUserId("u-1")).thenReturn(0L);
        when(repository.save(any(AttendeeStickerEntity.class)))
                .thenAnswer(inv -> {
                    AttendeeStickerEntity e = inv.getArgument(0);
                    e.setId(99L);
                    return e;
                });
        when(repository.findByUserId("u-1")).thenReturn(List.of());
        MockMultipartFile file = new MockMultipartFile("file", "s.png", "image/png", VALID_PNG_BYTES);

        StickerResponse response = service.upload(file, "u-1");

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getSticker()).startsWith("/sticker/u-1/u-1-");
        assertThat(response.getSticker()).endsWith(".png");

        try (var stream = Files.list(tempDir.resolve("sticker/u-1"))) {
            assertThat(stream.count()).isEqualTo(1L);
        }
    }

    @Test
    void uploadAcceptsGif() throws Exception {
        when(repository.countByUserId("u-1")).thenReturn(0L);
        when(repository.save(any(AttendeeStickerEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(repository.findByUserId("u-1")).thenReturn(List.of());
        MockMultipartFile file = new MockMultipartFile("file", "s.gif", "image/gif", VALID_GIF_BYTES);

        StickerResponse response = service.upload(file, "u-1");

        assertThat(response.getSticker()).endsWith(".gif");
    }

    @Test
    void uploadRejectsWhenLimitReached() throws Exception {
        when(repository.countByUserId("u-1")).thenReturn(5L);
        MockMultipartFile file = new MockMultipartFile("file", "s.png", "image/png", VALID_PNG_BYTES);

        assertThatThrownBy(() -> service.upload(file, "u-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sticker_limit_reached");

        verify(repository, never()).save(any());
        try (var stream = Files.list(tempDir.resolve("sticker"))) {
            assertThat(stream.count()).isEqualTo(0L);
        }
    }

    @Test
    void uploadRejectsUnsupportedMediaType() {
        when(repository.countByUserId("u-1")).thenReturn(0L);
        MockMultipartFile file = new MockMultipartFile("file", "s.svg", "image/svg+xml", "<svg/>".getBytes());

        assertThatThrownBy(() -> service.upload(file, "u-1"))
                .isInstanceOf(UnsupportedMediaTypeException.class)
                .hasMessage("unsupported_image_type");
    }

    @Test
    void uploadRejectsFileTooLarge() {
        when(repository.countByUserId("u-1")).thenReturn(0L);
        byte[] oversized = new byte[11 * 1024 * 1024];
        System.arraycopy(VALID_PNG_BYTES, 0, oversized, 0, VALID_PNG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", oversized);

        assertThatThrownBy(() -> service.upload(file, "u-1"))
                .isInstanceOf(PayloadTooLargeException.class)
                .hasMessage("file_too_large");
    }

    @Test
    void deleteByIdRemovesRowButKeepsFile() throws Exception {
        Files.createDirectories(tempDir.resolve("sticker/u-1"));
        Path existingFile = tempDir.resolve("sticker/u-1/u-1-250101000001-aaa.png");
        Files.write(existingFile, VALID_PNG_BYTES);

        AttendeeStickerEntity row = AttendeeStickerEntity.builder()
                .id(7L).userId("u-1").sticker("/sticker/u-1/u-1-250101000001-aaa.png").build();
        when(repository.findByIdAndUserId(7L, "u-1")).thenReturn(Optional.of(row));

        service.delete(7L, "u-1");

        verify(repository).delete(row);
        assertThat(Files.exists(existingFile)).isTrue();
    }

    @Test
    void deleteRejectsUnknownId() {
        when(repository.findByIdAndUserId(9L, "u-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9L, "u-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sticker_not_found");
    }

    @Test
    void uploadEvictsOldestNonActiveProtectingActive() throws Exception {
        // Create 15 pre-existing files; lexical == chronological (250101000001 oldest, 250101000015 newest)
        Path userDir = tempDir.resolve("sticker/u-1");
        Files.createDirectories(userDir);
        for (int i = 1; i <= 15; i++) {
            String ts = String.format("2501010000%02d", i);
            Files.write(userDir.resolve("u-1-" + ts + "-aaa.png"), VALID_PNG_BYTES);
        }

        // Mock: count is 0 (upload is allowed), save returns entity with id 50
        when(repository.countByUserId("u-1")).thenReturn(0L);
        when(repository.save(any(AttendeeStickerEntity.class)))
                .thenAnswer(inv -> {
                    AttendeeStickerEntity e = inv.getArgument(0);
                    e.setId(50L);
                    return e;
                });

        // File 01 is "active" (protected) — referenced by a DB row
        when(repository.findByUserId("u-1")).thenReturn(List.of(
                AttendeeStickerEntity.builder()
                        .id(1L).userId("u-1")
                        .sticker("/sticker/u-1/u-1-250101000001-aaa.png")
                        .build()
        ));

        // Upload new file → writes 16th file (ts is 2026xx, sorts newest)
        MockMultipartFile file = new MockMultipartFile("file", "s.png", "image/png", VALID_PNG_BYTES);
        service.upload(file, "u-1");

        // After upload: excess=1, file-01 protected, file-02 deleted, new file kept
        assertThat(Files.exists(userDir.resolve("u-1-250101000001-aaa.png"))).isTrue();
        assertThat(Files.exists(userDir.resolve("u-1-250101000002-aaa.png"))).isFalse();

        try (var stream = Files.list(userDir)) {
            assertThat(stream.collect(Collectors.toList())).hasSize(15);
        }
    }
}
