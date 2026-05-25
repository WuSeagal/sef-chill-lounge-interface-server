package com.sef.cli.chat.event.request;

import com.sef.cli.message.enums.MessageType;

import java.util.List;

public record ChatMessageSendRequest(
        MessageType messageType,
        String content,
        List<String> imageUrls
) {
}
