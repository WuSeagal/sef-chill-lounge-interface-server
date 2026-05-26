package com.sef.cli.image.service;

public final class MagicByteValidator {

    private MagicByteValidator() {}

    public static ImageFormat detectFormat(byte[] header) {
        if (header == null || header.length < 12) return null;

        if (b(header, 0) == 0xFF && b(header, 1) == 0xD8 && b(header, 2) == 0xFF) {
            return ImageFormat.JPEG;
        }
        if (b(header, 0) == 0x89 && b(header, 1) == 0x50 && b(header, 2) == 0x4E
                && b(header, 3) == 0x47 && b(header, 4) == 0x0D && b(header, 5) == 0x0A
                && b(header, 6) == 0x1A && b(header, 7) == 0x0A) {
            return ImageFormat.PNG;
        }
        if (b(header, 0) == 0x47 && b(header, 1) == 0x49 && b(header, 2) == 0x46
                && b(header, 3) == 0x38 && (b(header, 4) == 0x37 || b(header, 4) == 0x39)
                && b(header, 5) == 0x61) {
            return ImageFormat.GIF;
        }
        if (b(header, 0) == 'R' && b(header, 1) == 'I' && b(header, 2) == 'F' && b(header, 3) == 'F'
                && b(header, 8) == 'W' && b(header, 9) == 'E' && b(header, 10) == 'B' && b(header, 11) == 'P') {
            return ImageFormat.WEBP;
        }
        return null;
    }

    private static int b(byte[] arr, int i) {
        return arr[i] & 0xFF;
    }
}
