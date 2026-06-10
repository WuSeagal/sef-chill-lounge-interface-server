package com.sef.cli.image.service;

import ch.qos.logback.classic.Level;
import com.sef.cli.image.properties.ImageStorageProperties;
import com.sef.cli.image.web.dto.AvatarUploadResponse;
import com.sef.cli.image.web.exception.PayloadTooLargeException;
import com.sef.cli.image.web.exception.UnsupportedMediaTypeException;
import com.sef.cli.testutil.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvatarUploadServiceTest {

    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final byte[] VALID_JPG_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    @TempDir
    Path tempDir;

    private AvatarUploadService service;

    @BeforeEach
    void setUp() throws Exception {
        ImageStorageProperties props = new ImageStorageProperties();
        props.setBasePath(tempDir.toString() + "/");
        props.setMaxFileSizeMb(10);
        props.getUser().setUrlPrefix("/user/");
        Files.createDirectories(tempDir.resolve("user"));
        service = new AvatarUploadService(props);
    }

    @Test
    void uploadStoresAvatarUsingFixedUserFileName() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fox.png", "image/png", VALID_PNG_BYTES);

        AvatarUploadResponse response = service.upload(file, "google-123");

        assertThat(response.avatarPath()).startsWith("/user/google-123.png?v=");
        assertThat(Files.exists(tempDir.resolve("user/google-123.png"))).isTrue();
        assertThat(Files.readAllBytes(tempDir.resolve("user/google-123.png"))).isEqualTo(VALID_PNG_BYTES);
    }

    @Test
    void uploadDeletesOldAvatarWithDifferentExtension() throws Exception {
        Files.write(tempDir.resolve("user/google-123.jpg"), VALID_JPG_BYTES);
        MockMultipartFile file = new MockMultipartFile(
                "file", "fox.png", "image/png", VALID_PNG_BYTES);

        service.upload(file, "google-123");

        assertThat(Files.exists(tempDir.resolve("user/google-123.jpg"))).isFalse();
        assertThat(Files.exists(tempDir.resolve("user/google-123.png"))).isTrue();
    }

    @Test
    void uploadRejectsUnsupportedMediaType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fox.svg", "image/svg+xml", "<svg />".getBytes());

        assertThatThrownBy(() -> service.upload(file, "google-123"))
                .isInstanceOf(UnsupportedMediaTypeException.class)
                .hasMessage("unsupported_image_type");
    }

    @Test
    void uploadRejectsFileTooLarge() {
        byte[] oversized = new byte[11 * 1024 * 1024];
        oversized[0] = (byte) 0x89;
        oversized[1] = 0x50;
        oversized[2] = 0x4E;
        oversized[3] = 0x47;
        oversized[4] = 0x0D;
        oversized[5] = 0x0A;
        oversized[6] = 0x1A;
        oversized[7] = 0x0A;
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", oversized);

        assertThatThrownBy(() -> service.upload(file, "google-123"))
                .isInstanceOf(PayloadTooLargeException.class)
                .hasMessage("file_too_large");
    }

    @Test
    void logsInfoOnSuccessfulUpload() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fox.png", "image/png", VALID_PNG_BYTES);

        try (LogCaptor captor = LogCaptor.forClass(AvatarUploadService.class)) {
            service.upload(file, "google-123");
            captor.assertLogged(Level.INFO, "[AVATAR_UPLOAD]",
                    "userId=google-123", "google-123.png", "size=" + VALID_PNG_BYTES.length);
        }
    }

    @Test
    void logsWarnWhenFileTooLarge() {
        byte[] oversized = new byte[11 * 1024 * 1024];
        oversized[0] = (byte) 0x89;
        oversized[1] = 0x50;
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", oversized);

        try (LogCaptor captor = LogCaptor.forClass(AvatarUploadService.class)) {
            assertThatThrownBy(() -> service.upload(file, "google-123"))
                    .isInstanceOf(PayloadTooLargeException.class);
            captor.assertLogged(Level.WARN, "[AVATAR_UPLOAD_FAIL]", "userId=google-123");
        }
    }

    @Test
    void logsWarnWhenUnsupportedMediaType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fox.svg", "image/svg+xml", "<svg />".getBytes());

        try (LogCaptor captor = LogCaptor.forClass(AvatarUploadService.class)) {
            assertThatThrownBy(() -> service.upload(file, "google-123"))
                    .isInstanceOf(UnsupportedMediaTypeException.class);
            captor.assertLogged(Level.WARN, "[AVATAR_UPLOAD_FAIL]", "userId=google-123");
        }
    }
}
