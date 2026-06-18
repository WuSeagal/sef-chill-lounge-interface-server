package com.sef.cli.chat.event;

public enum ChatEventType {
    CHAT_MESSAGE,
    PRESENCE_SNAPSHOT,
    PROFILE_UPDATED,
    PING,
    PONG,
    KICKED,
    ERROR,
    RATE_LIMITED,
    MESSAGE_DELETED,
    ANNOUNCEMENT,
    TYPING
}
