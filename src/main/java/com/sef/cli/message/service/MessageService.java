package com.sef.cli.message.service;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.message.entity.MessageEntity;
import com.sef.cli.message.enums.MessageType;
import com.sef.cli.message.repository.MessageRepository;
import com.sef.cli.message.service.dto.MessageHistoryData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final AttendeeDataRepository attendeeDataRepository;

    public MessageEntity persistText(String userId, String content, List<String> imageUrls) {
        String normalizedContent = content == null ? "" : content.trim();
        List<String> normalizedImages = imageUrls == null ? List.of() : List.copyOf(imageUrls);

        if (normalizedContent.isBlank() && normalizedImages.isEmpty()) {
            throw new IllegalArgumentException("message_content_required");
        }

        if (normalizedImages.size() > 5) {
            throw new IllegalArgumentException("message_images_limit_exceeded");
        }

        return messageRepository.save(MessageEntity.builder()
                .userId(userId)
                .messageType(MessageType.TEXT)
                .content(normalizedContent.isBlank() ? null : normalizedContent)
                .imageUrls(normalizedImages.isEmpty() ? null : normalizedImages)
                .build());
    }

    public MessageEntity persistSticker(String userId, String stickerImageUrl) {
        if (stickerImageUrl == null || stickerImageUrl.isBlank()) {
            throw new IllegalArgumentException("sticker_image_url_required");
        }

        return messageRepository.save(MessageEntity.builder()
                .userId(userId)
                .messageType(MessageType.STICKER)
                .stickerImageUrl(stickerImageUrl.trim())
                .build());
    }

    public List<MessageHistoryData> loadHistory(LocalDateTime before, Long beforeId, int limit) {
        int cappedLimit = Math.min(Math.max(limit, 1), 100);

        List<MessageEntity> entities = before == null
                ? messageRepository.findAllByOrderByCreatedDateDescIdDesc(PageRequest.of(0, cappedLimit))
                : messageRepository.findHistoryBefore(
                        before,
                        beforeId == null ? Long.MAX_VALUE : beforeId,
                        PageRequest.of(0, cappedLimit)
                );

        Map<String, AttendeeDataEntity> attendeeMap = attendeeDataRepository.findByUserIdIn(
                        entities.stream().map(MessageEntity::getUserId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(AttendeeDataEntity::getUserId, Function.identity()));

        return entities.stream()
                .map(entity -> MessageHistoryData.from(entity, attendeeMap.get(entity.getUserId())))
                .toList();
    }
}
