package com.sef.cli.image.service;

import java.util.Set;

public enum ImageFormat {
    JPEG("jpg", Set.of("jpg", "jpeg"), "image/jpeg"),
    PNG("png", Set.of("png"), "image/png"),
    GIF("gif", Set.of("gif"), "image/gif"),
    WEBP("webp", Set.of("webp"), "image/webp");

    private final String normalizedExt;
    private final Set<String> allowedExts;
    private final String mimeType;

    ImageFormat(String normalizedExt, Set<String> allowedExts, String mimeType) {
        this.normalizedExt = normalizedExt;
        this.allowedExts = allowedExts;
        this.mimeType = mimeType;
    }

    public String normalizedExtension() {
        return normalizedExt;
    }

    public boolean matchesExtension(String ext) {
        return ext != null && allowedExts.contains(ext.toLowerCase());
    }

    public boolean matchesMime(String mime) {
        return mime != null && mime.equalsIgnoreCase(mimeType);
    }

    public static ImageFormat matchExtension(String ext) {
        if (ext == null) return null;
        for (ImageFormat f : values()) {
            if (f.matchesExtension(ext)) return f;
        }
        return null;
    }
}
