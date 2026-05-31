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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    void uploadStoresStickerInPerUserSlotFileAndUpserts() throws Exception {
        when(repository.findByUserIdAndStickerNo("u-1", 1)).thenReturn(Optional.empty());
        when(repository.save(any(AttendeeStickerEntity.class)))
                .thenAnswer(inv -> { AttendeeStickerEntity e = inv.getArgument(0); e.setId(99L); return e; });
        MockMultipartFile file = new MockMultipartFile("file", "s.png", "image/png", VALID_PNG_BYTES);

        StickerResponse response = service.upload(file, "u-1", 1);

        assertThat(response.getStickerNo()).isEqualTo(1);
        assertThat(response.getSticker()).startsWith("/sticker/u-1/1.png?v=");
        assertThat(Files.exists(tempDir.resolve("sticker/u-1/1.png"))).isTrue();
    }

    @Test
    void uploadAcceptsGif() throws Exception {
        when(repository.findByUserIdAndStickerNo("u-1", 2)).thenReturn(Optional.empty());
        when(repository.save(any(AttendeeStickerEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "s.gif", "image/gif", VALID_GIF_BYTES);

        StickerResponse response = service.upload(file, "u-1", 2);

        assertThat(response.getSticker()).startsWith("/sticker/u-1/2.gif?v=");
    }

    @Test
    void uploadUpdatesExistingRowAndDeletesOldExtensionFile() throws Exception {
        Files.createDirectories(tempDir.resolve("sticker/u-1"));
        Files.write(tempDir.resolve("sticker/u-1/1.gif"), VALID_GIF_BYTES);
        when(repository.findByUserIdAndStickerNo("u-1", 1)).thenReturn(Optional.of(
                AttendeeStickerEntity.builder().id(5L).userId("u-1").stickerNo(1).sticker("/sticker/u-1/1.gif?v=1").build()));
        when(repository.save(any(AttendeeStickerEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "s.png", "image/png", VALID_PNG_BYTES);

        StickerResponse response = service.upload(file, "u-1", 1);

        assertThat(Files.exists(tempDir.resolve("sticker/u-1/1.gif"))).isFalse();
        assertThat(Files.exists(tempDir.resolve("sticker/u-1/1.png"))).isTrue();
        assertThat(response.getId()).isEqualTo(5L);
    }

    @Test
    void uploadRejectsSlotOutOfRange() {
        MockMultipartFile file = new MockMultipartFile("file", "s.png", "image/png", VALID_PNG_BYTES);
        assertThatThrownBy(() -> service.upload(file, "u-1", 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("sticker_slot_out_of_range");
        assertThatThrownBy(() -> service.upload(file, "u-1", 6))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("sticker_slot_out_of_range");
    }

    @Test
    void uploadRejectsUnsupportedMediaType() {
        MockMultipartFile file = new MockMultipartFile("file", "s.svg", "image/svg+xml", "<svg/>".getBytes());
        assertThatThrownBy(() -> service.upload(file, "u-1", 1))
                .isInstanceOf(UnsupportedMediaTypeException.class).hasMessage("unsupported_image_type");
    }

    @Test
    void uploadRejectsFileTooLarge() {
        byte[] oversized = new byte[11 * 1024 * 1024];
        System.arraycopy(VALID_PNG_BYTES, 0, oversized, 0, VALID_PNG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", oversized);
        assertThatThrownBy(() -> service.upload(file, "u-1", 1))
                .isInstanceOf(PayloadTooLargeException.class).hasMessage("file_too_large");
    }

    @Test
    void deleteRemovesSlotFilesAndRow() throws Exception {
        Files.createDirectories(tempDir.resolve("sticker/u-1"));
        Files.write(tempDir.resolve("sticker/u-1/3.png"), VALID_PNG_BYTES);
        AttendeeStickerEntity existing = AttendeeStickerEntity.builder()
                .id(7L).userId("u-1").stickerNo(3).sticker("/sticker/u-1/3.png?v=1").build();
        when(repository.findByUserIdAndStickerNo("u-1", 3)).thenReturn(Optional.of(existing));

        service.delete("u-1", 3);

        assertThat(Files.exists(tempDir.resolve("sticker/u-1/3.png"))).isFalse();
        verify(repository).delete(existing);
    }

    @Test
    void deleteRejectsSlotOutOfRange() {
        assertThatThrownBy(() -> service.delete("u-1", 9))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("sticker_slot_out_of_range");
    }
}
