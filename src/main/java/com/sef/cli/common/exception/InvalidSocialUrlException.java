package com.sef.cli.common.exception;

import com.sef.cli.attendee.service.SocialUrlValidator;

public class InvalidSocialUrlException extends RuntimeException {

    private final SocialUrlValidator.Result reason;

    public InvalidSocialUrlException(SocialUrlValidator.Result reason) {
        super("invalid_social_url");
        this.reason = reason;
    }

    public SocialUrlValidator.Result getReason() {
        return reason;
    }

    /** 依驗證結果回傳精確錯誤碼，讓前端可區分（長度過長 / 平台不符 / 不安全等） */
    public String getErrorCode() {
        return switch (reason) {
            case UNSAFE_URL -> "unsafe_social_url";
            case PLATFORM_MISMATCH -> "social_url_platform_mismatch";
            case TOO_LONG -> "social_url_too_long";
            default -> "invalid_social_url";
        };
    }
}
