package com.sef.cli.attendee.service;

import com.sef.cli.attendee.enums.PlatformEnum;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Pattern;

@Component
public class SocialUrlValidator {

    public enum Result { OK, INVALID_URL, UNSAFE_URL, PLATFORM_MISMATCH, TOO_LONG }

    /** 社群連結最大長度（前後端一致，避免濫用超長字串） */
    public static final int MAX_LINKS_LENGTH = 200;

    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    public Result validate(PlatformEnum platform, String raw) {
        if (raw != null && raw.length() > MAX_LINKS_LENGTH) {
            return Result.TOO_LONG;
        }
        URI uri;
        try {
            uri = new URI(raw);
        } catch (Exception e) {
            return Result.INVALID_URL;
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return Result.UNSAFE_URL;
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return Result.INVALID_URL;
        }
        host = host.toLowerCase();
        if (isUnsafeHost(host)) {
            return Result.UNSAFE_URL;
        }
        if (platform.hasHostPattern() && !platform.getUrlHostPattern().matcher(host).matches()) {
            return Result.PLATFORM_MISMATCH;
        }
        return Result.OK;
    }

    private boolean isUnsafeHost(String host) {
        if (host.equals("localhost") || host.endsWith(".localhost")) {
            return true;
        }
        if (IPV4.matcher(host).matches()) {
            return true;
        }
        return host.startsWith("[") && host.endsWith("]");
    }
}
