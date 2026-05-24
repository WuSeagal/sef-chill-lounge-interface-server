package com.sef.cli.common.exception;

public class InvalidTopicIdException extends RuntimeException {
    public InvalidTopicIdException() {
        super("invalid_topic_id");
    }
}
