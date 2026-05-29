package com.sef.cli.chat.event.response;

import com.sef.cli.message.enums.MessageType;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageBroadcast(
        Long cursorId,
        String messageId,
        String userId,
        String furName,
        String avatar,
        String avatarColor,
        boolean avatarBorder,
        MessageType messageType,
        String content,
        List<String> imageUrls,
        String stickerImageUrl,
        LocalDateTime createdDate
) {
}
