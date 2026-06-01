package com.sef.cli.chat.event.response;

public record ProfileUpdatedPayload(String userId, String furName, String avatar, String avatarColor, boolean avatarBorder) {
}
