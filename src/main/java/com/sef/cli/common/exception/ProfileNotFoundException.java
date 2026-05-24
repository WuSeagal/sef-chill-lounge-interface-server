package com.sef.cli.common.exception;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException() {
        super("profile_not_found");
    }
}
