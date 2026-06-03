package com.sef.cli.common.exception;

public class InvalidPlatformException extends RuntimeException {
    public InvalidPlatformException(String platform) {
        super("invalid_platform");
    }
}
