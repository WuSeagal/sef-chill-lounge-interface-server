package com.sef.cli.common.exception;

public class TagAlreadyAssociatedException extends RuntimeException {
    public TagAlreadyAssociatedException() {
        super("tag_already_associated");
    }
}
