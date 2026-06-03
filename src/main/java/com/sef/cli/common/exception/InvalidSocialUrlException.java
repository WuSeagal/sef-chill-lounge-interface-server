package com.sef.cli.common.exception;

import com.sef.cli.attendee.service.SocialUrlValidator;

public class InvalidSocialUrlException extends RuntimeException {
    public InvalidSocialUrlException(SocialUrlValidator.Result reason) {
        super("invalid_social_url");
    }
}
