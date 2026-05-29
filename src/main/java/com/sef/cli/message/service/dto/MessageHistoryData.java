package com.sef.cli.message.service.dto;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.message.entity.MessageEntity;
import com.sef.cli.message.enums.MessageType;

import java.time.LocalDateTime;
import java.util.List;

public record MessageHistoryData(
        Long cursorId,
        String messageId,
        String userId,
        MessageType messageType,
        String furName,
        String avatar,
        String avatarColor,
        boolean avatarBorder,
        String content,
        List<String> imageUrls,
        String stickerImageUrl,
        LocalDateTime createdDate
) {

    public static MessageHistoryData from(MessageEntity entity, AttendeeDataEntity attendee) {
        return new MessageHistoryData(
                entity.getId(),
                entity.getMessageId(),
                entity.getUserId(),
                entity.getMessageType(),
                attendee == null ? null : attendee.getFurName(),
                attendee == null ? null : attendee.getAvatar(),
                attendee == null ? null : attendee.getAvatarColor(),
                attendee != null && attendee.isAvatarBorder(),
                entity.getContent(),
                entity.getImageUrls() == null ? List.of() : List.copyOf(entity.getImageUrls()),
                entity.getStickerImageUrl(),
                entity.getCreatedDate()
        );
    }
}
