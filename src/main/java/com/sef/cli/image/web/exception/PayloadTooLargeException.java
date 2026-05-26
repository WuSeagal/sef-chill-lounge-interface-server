package com.sef.cli.image.web.exception;

import lombok.Getter;

@Getter
public class PayloadTooLargeException extends RuntimeException {
    private final int maxSizeMb;

    public PayloadTooLargeException(String message, int maxSizeMb) {
        super(message);
        this.maxSizeMb = maxSizeMb;
    }
}
