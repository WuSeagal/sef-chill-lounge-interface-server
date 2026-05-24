package com.sef.cli.common.exception;

public class ProfileAlreadyExistsException extends RuntimeException {
    public ProfileAlreadyExistsException() {
        super("profile_already_exists");
    }
}
