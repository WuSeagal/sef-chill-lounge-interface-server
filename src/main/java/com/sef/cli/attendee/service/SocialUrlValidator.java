package com.sef.cli.attendee.service;

import com.sef.cli.attendee.enums.PlatformEnum;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Pattern;

@Component
public class SocialUrlValidator {

    public enum Result { OK, INVALID_URL, UNSAFE_URL, PLATFORM_MISMATCH }

    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    public Result validate(PlatformEnum platform, String raw) {
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
