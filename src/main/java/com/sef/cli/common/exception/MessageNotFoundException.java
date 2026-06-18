package com.sef.cli.common.exception;

public class MessageNotFoundException extends RuntimeException {
    public MessageNotFoundException() {
        super("message_not_found");
    }
}
