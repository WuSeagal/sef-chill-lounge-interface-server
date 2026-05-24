package com.sef.cli.common.exception;

public class TagJunctionNotFoundException extends RuntimeException {
    public TagJunctionNotFoundException() {
        super("tag_junction_not_found");
    }
}
