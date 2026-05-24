package com.sef.cli.common.exception;

public class NoOtherTopicAvailableException extends RuntimeException {
    public NoOtherTopicAvailableException() {
        super("no_other_topic_available");
    }
}
