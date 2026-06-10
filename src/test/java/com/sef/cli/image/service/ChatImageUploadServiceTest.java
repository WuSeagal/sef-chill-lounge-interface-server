package com.sef.cli.image.service;

import ch.qos.logback.classic.Level;
import com.sef.cli.image.entity.ChatImageAssetEntity;
import com.sef.cli.image.properties.ImageStorageProperties;
import com.sef.cli.image.repository.ChatImageAssetRepository;
import com.sef.cli.image.web.dto.ChatImageUploadResponse;
import com.sef.cli.image.web.exception.PayloadTooLargeException;
import com.sef.cli.image.web.exception.UnsupportedMediaTypeException;
import com.sef.cli.testutil.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatImageUploadServiceTest {

    @Mock
    private ChatImageAssetRepository repository;

    @Mock
    private ChatImageRetentionService retention;

    @TempDir
    Path tempDir;

    private ChatImageUploadService service;
    private ImageStorageProperties props;

    @BeforeEach
    void setUp() throws Exception {
        props = new ImageStorageProperties();
        props.setBasePath(tempDir.toString() + "/");
        props.setMaxFileSizeMb(10);
        props.getChat().setUrlPrefix("/image/");
        Files.createDirectories(tempDir.resolve("image"));
        service = new ChatImageUploadService(repository, retention, props);
    }

    @Test
    void uploadsValidPngWritesFileAndDbRowAndReturnsUrl() throws Exception {
        byte[] png = pngHeaderPadded();
        MockMultipartFile file = new MockMultipartFile(
                "file", "anything.png", "image/png", png);

        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatImageUploadResponse resp = service.upload(file, "attendee_abcdef123");

        assertThat(resp.fileName()).matches("def123-\\d{12}-[a-zA-Z0-9_-]{3}\\.png");
        assertThat(resp.url()).startsWith("/image/").endsWith(".png");

        Path written = tempDir.resolve("image").resolve(resp.fileName());
        assertThat(Files.exists(written)).isTrue();
        assertThat(Files.readAllBytes(written)).isEqualTo(png);

        ArgumentCaptor<ChatImageAssetEntity> captor = ArgumentCaptor.forClass(ChatImageAssetEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUploadedBy()).isEqualTo("attendee_abcdef123");
        assertThat(captor.getValue().getFileName()).isEqualTo(resp.fileName());

        verify(retention).evictOldestIfOver();
    }

    @Test
    void rejectsFileTooLarge() {
        byte[] big = new byte[11 * 1024 * 1024];
        big[0] = (byte) 0x89; big[1] = 0x50; big[2] = 0x4E; big[3] = 0x47;
        big[4] = 0x0D; big[5] = 0x0A; big[6] = 0x1A; big[7] = 0x0A;
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);

        assertThatThrownBy(() -> service.upload(file, "u-1"))
                .isInstanceOf(PayloadTooLargeException.class)
                .hasMessage("file_too_large");
    }

    @Test
    void rejectsUnsupportedExtension() {
        byte[] dummy = new byte[16];
        MockMultipartFile file = new MockMultipartFile("file", "a.bmp", "image/bmp", dummy);

        assertThatThrownBy(() -> service.upload(file, "u-1"))
                .isInstanceOf(UnsupportedMediaTypeException.class)
                .hasMessage("unsupported_image_type");
    }

    @Test
    void rejectsSvgExtension() {
        byte[] dummy = new byte[16];
        MockMultipartFile file = new MockMultipartFile("file", "evil.svg", "image/svg+xml", dummy);

        assertThatThrownBy(() -> service.upload(file, "u-1"))
                .isInstanceOf(UnsupportedMediaTypeException.class);
    }

    @Test
    void rejectsContentTypeMismatch() {
        byte[] png = pngHeaderPadded();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "application/octet-stream", png);

        assertThatThrownBy(() -> service.upload(file, "u-1"))
                .isInstanceOf(UnsupportedMediaTypeException.class);
    }

    @Test
    void rejectsExeRenamedAsJpg() {
        byte[] pe32 = new byte[16];
        pe32[0] = 0x4D; pe32[1] = 0x5A;
        MockMultipartFile file = new MockMultipartFile("file", "evil.jpg", "image/jpeg", pe32);

        assertThatThrownBy(() -> service.upload(file, "u-1"))
                .isInstanceOf(UnsupportedMediaTypeException.class)
                .hasMessage("unsupported_image_type");
    }

    @Test
    void deletesWrittenFileWhenDbSaveFails() throws Exception {
        byte[] png = pngHeaderPadded();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", png);
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("UNIQUE"));

        assertThatThrownBy(() -> service.upload(file, "u-1"))
                .isInstanceOf(DataIntegrityViolationException.class);

        // 寫入的檔案應該被主動刪除（避免孤兒）
        long remaining = Files.list(tempDir.resolve("image")).count();
        assertThat(remaining).isZero();
    }

    @Test
    void filenameUsesAllOfShortUserId() {
        byte[] png = pngHeaderPadded();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", png);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatImageUploadResponse resp = service.upload(file, "shortid");

        assertThat(resp.fileName()).startsWith("hortid-").endsWith(".png");
    }

    @Test
    void logsInfoOnSuccessfulUpload() {
        byte[] png = pngHeaderPadded();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", png);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try (LogCaptor captor = LogCaptor.forClass(ChatImageUploadService.class)) {
            ChatImageUploadResponse resp = service.upload(file, "attendee_abcdef123");
            captor.assertLogged(Level.INFO, "[CHAT_IMAGE_UPLOAD]",
                    "userId=attendee_abcdef123", resp.fileName(), "size=" + png.length);
        }
    }

    @Test
    void logsWarnWhenFileTooLarge() {
        byte[] big = new byte[11 * 1024 * 1024];
        big[0] = (byte) 0x89; big[1] = 0x50; big[2] = 0x4E; big[3] = 0x47;
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);

        try (LogCaptor captor = LogCaptor.forClass(ChatImageUploadService.class)) {
            assertThatThrownBy(() -> service.upload(file, "u-1"))
                    .isInstanceOf(PayloadTooLargeException.class);
            captor.assertLogged(Level.WARN, "[CHAT_IMAGE_UPLOAD_FAIL]", "userId=u-1");
        }
    }

    @Test
    void logsWarnWhenUnsupportedMediaType() {
        byte[] dummy = new byte[16];
        MockMultipartFile file = new MockMultipartFile("file", "a.bmp", "image/bmp", dummy);

        try (LogCaptor captor = LogCaptor.forClass(ChatImageUploadService.class)) {
            assertThatThrownBy(() -> service.upload(file, "u-1"))
                    .isInstanceOf(UnsupportedMediaTypeException.class);
            captor.assertLogged(Level.WARN, "[CHAT_IMAGE_UPLOAD_FAIL]", "userId=u-1");
        }
    }

    private byte[] pngHeaderPadded() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0, 0, 0, 0, 0, 0, 0, 0
        };
    }
}
