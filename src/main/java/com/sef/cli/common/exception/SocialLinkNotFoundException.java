package com.sef.cli.common.exception;

public class SocialLinkNotFoundException extends RuntimeException {
    public SocialLinkNotFoundException() {
        super("social_link_not_found");
    }
}
