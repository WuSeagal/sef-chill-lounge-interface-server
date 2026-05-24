package com.sef.cli.common.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException() {
        super("forbidden");
    }
}
