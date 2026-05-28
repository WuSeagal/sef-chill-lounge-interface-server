package com.sef.cli.common.exception;

public class TagLimitExceededException extends RuntimeException {
    public TagLimitExceededException() {
        super("tag_limit_exceeded");
    }
}
