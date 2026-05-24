package com.sef.cli.common.exception;

public class NoTopicAvailableException extends RuntimeException {
    public NoTopicAvailableException() {
        super("no_topic_available");
    }
}
