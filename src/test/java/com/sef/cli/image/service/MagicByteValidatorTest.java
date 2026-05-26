package com.sef.cli.image.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MagicByteValidatorTest {

    @Test
    void detectsJpegByMagicBytes() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
        assertThat(MagicByteValidator.detectFormat(jpeg)).isEqualTo(ImageFormat.JPEG);
    }

    @Test
    void detectsPngByMagicBytes() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        assertThat(MagicByteValidator.detectFormat(png)).isEqualTo(ImageFormat.PNG);
    }

    @Test
    void detectsGifBothVersions() {
        byte[] gif87 = padTo12("GIF87a".getBytes());
        byte[] gif89 = padTo12("GIF89a".getBytes());
        assertThat(MagicByteValidator.detectFormat(gif87)).isEqualTo(ImageFormat.GIF);
        assertThat(MagicByteValidator.detectFormat(gif89)).isEqualTo(ImageFormat.GIF);
    }

    @Test
    void detectsWebpByRiffAndWebpMarker() {
        byte[] webp = new byte[12];
        System.arraycopy("RIFF".getBytes(), 0, webp, 0, 4);
        System.arraycopy("WEBP".getBytes(), 0, webp, 8, 4);
        assertThat(MagicByteValidator.detectFormat(webp)).isEqualTo(ImageFormat.WEBP);
    }

    @Test
    void rejectsPe32ExeWithJpegExtension() {
        byte[] pe32 = new byte[]{0x4D, 0x5A, (byte) 0x90, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        assertThat(MagicByteValidator.detectFormat(pe32)).isNull();
    }

    @Test
    void rejectsSvgWithImageExtension() {
        byte[] svg = padTo12("<?xml ver".getBytes());
        assertThat(MagicByteValidator.detectFormat(svg)).isNull();
    }

    @Test
    void rejectsBufferTooShort() {
        assertThat(MagicByteValidator.detectFormat(new byte[]{0x12, 0x34})).isNull();
    }

    @Test
    void formatMatchesExtensionJpegToJpgAndJpeg() {
        assertThat(ImageFormat.JPEG.matchesExtension("jpg")).isTrue();
        assertThat(ImageFormat.JPEG.matchesExtension("jpeg")).isTrue();
        assertThat(ImageFormat.JPEG.matchesExtension("JPEG")).isTrue();
        assertThat(ImageFormat.JPEG.matchesExtension("png")).isFalse();
        assertThat(ImageFormat.JPEG.normalizedExtension()).isEqualTo("jpg");
    }

    @Test
    void matchExtensionFindsFormat() {
        assertThat(ImageFormat.matchExtension("png")).isEqualTo(ImageFormat.PNG);
        assertThat(ImageFormat.matchExtension("webp")).isEqualTo(ImageFormat.WEBP);
        assertThat(ImageFormat.matchExtension("bmp")).isNull();
        assertThat(ImageFormat.matchExtension(null)).isNull();
    }

    @Test
    void matchesMimeCaseInsensitive() {
        assertThat(ImageFormat.JPEG.matchesMime("image/jpeg")).isTrue();
        assertThat(ImageFormat.JPEG.matchesMime("IMAGE/JPEG")).isTrue();
        assertThat(ImageFormat.JPEG.matchesMime("image/png")).isFalse();
    }

    private byte[] padTo12(byte[] src) {
        byte[] out = new byte[12];
        System.arraycopy(src, 0, out, 0, Math.min(src.length, 12));
        return out;
    }
}
