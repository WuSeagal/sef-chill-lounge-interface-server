package com.sef.cli.message.service;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.common.HostAuthz;
import com.sef.cli.common.exception.ForbiddenException;
import com.sef.cli.common.exception.MessageNotFoundException;
import com.sef.cli.message.entity.MessageEntity;
import com.sef.cli.message.enums.MessageType;
import com.sef.cli.message.repository.MessageRepository;
import com.sef.cli.message.service.dto.MessageHistoryData;
import com.sef.cli.message.service.dto.ReplyPreview;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final AttendeeDataRepository attendeeDataRepository;
    private final MessageIdGenerator messageIdGenerator;

    public MessageEntity persistText(String userId, String content, List<String> imageUrls) {
        return persistText(userId, content, imageUrls, null);
    }

    public MessageEntity persistText(String userId, String content, List<String> imageUrls, String replyToMessageId) {
        String normalizedContent = content == null ? "" : content.trim();
        List<String> normalizedImages = imageUrls == null ? List.of() : List.copyOf(imageUrls);

        if (normalizedContent.length() > 500) {
            throw new IllegalArgumentException("message_content_too_long");
        }

        if (normalizedContent.isBlank() && normalizedImages.isEmpty()) {
            throw new IllegalArgumentException("message_content_required");
        }

        if (normalizedImages.size() > 5) {
            throw new IllegalArgumentException("message_images_limit_exceeded");
        }

        for (String url : normalizedImages) {
            if (url == null || !url.startsWith("/image/")) {
                throw new IllegalArgumentException("message_image_url_invalid_prefix");
            }
        }

        // 只原樣儲存 replyToMessageId（trim 後空白視為 null），不在寫入時查詢或即時解析任何被
        // 回覆訊息的資料——作者/摘要/時間一律於讀取歷史或組裝廣播時由 resolveReplyPreview* 即時
        // 解析，以保證改名/刪除後每次讀取都反映當前狀態。
        String normalizedReplyToMessageId = normalizeReplyToMessageId(replyToMessageId);

        return messageRepository.save(MessageEntity.builder()
                .messageId(messageIdGenerator.generate())
                .userId(userId)
                .messageType(MessageType.TEXT)
                .content(normalizedContent.isBlank() ? null : normalizedContent)
                .imageUrls(normalizedImages.isEmpty() ? null : normalizedImages)
                .replyToMessageId(normalizedReplyToMessageId)
                .build());
    }

    private static final int REPLY_TO_MESSAGE_ID_MAX = 64;

    // 與 content/imageUrls 同層級的輸入驗證：欄位對應 DB VARCHAR(64)，超長應在此擋下並回清楚的
    // IllegalArgumentException（走既有 ERROR envelope 路徑），而非讓持久化層丟未處理的例外。
    private String normalizeReplyToMessageId(String replyToMessageId) {
        if (replyToMessageId == null || replyToMessageId.isBlank()) {
            return null;
        }
        String trimmed = replyToMessageId.trim();
        if (trimmed.length() > REPLY_TO_MESSAGE_ID_MAX) {
            throw new IllegalArgumentException("reply_to_message_id_invalid");
        }
        return trimmed;
    }

    private static final int REPLY_SNIPPET_MAX = 50;

    private String snippetOf(MessageEntity target) {
        if (target.getMessageType() == MessageType.STICKER) {
            return "[貼圖]";
        }
        String content = target.getContent();
        if (content == null || content.isBlank()) {
            return "[圖片]";
        }
        String trimmed = content.strip();
        return trimmed.length() > REPLY_SNIPPET_MAX
                ? trimmed.substring(0, REPLY_SNIPPET_MAX) + "…"
                : trimmed;
    }

    /**
     * 單則回覆預覽解析（供 {@code ChatWebSocketHandler} 組裝單則廣播用）。委派批次版本以共用
     * 「排除已刪除目標」與摘要規則，避免兩處各自實作而行為漂移。
     */
    public ReplyPreview resolveReplyPreview(String replyToMessageId) {
        if (replyToMessageId == null || replyToMessageId.isBlank()) {
            return null;
        }
        return resolveReplyPreviews(Set.of(replyToMessageId)).get(replyToMessageId);
    }

    /**
     * 批次回覆預覽解析（供 {@code loadHistory} 用）：一次查詢取得所有未刪除的目標訊息，
     * 目標作者的 furName 亦以批次查詢取得（獨立於呼叫端已知的其他 userId 集合，
     * 確保目標作者未出現在呼叫端集合中時仍能正確解析）。
     */
    public Map<String, ReplyPreview> resolveReplyPreviews(Set<String> replyToMessageIds) {
        if (replyToMessageIds == null || replyToMessageIds.isEmpty()) {
            return Map.of();
        }

        List<MessageEntity> targets = messageRepository.findByMessageIdInAndDeletedFalse(replyToMessageIds);
        if (targets.isEmpty()) {
            return Map.of();
        }

        Map<String, AttendeeDataEntity> targetAttendeeMap = attendeeDataRepository.findByUserIdIn(
                        targets.stream().map(MessageEntity::getUserId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(AttendeeDataEntity::getUserId, Function.identity()));

        Map<String, ReplyPreview> result = new HashMap<>();
        for (MessageEntity target : targets) {
            String furName = targetAttendeeMap.containsKey(target.getUserId())
                    ? targetAttendeeMap.get(target.getUserId()).getFurName()
                    : null;
            result.put(target.getMessageId(),
                    new ReplyPreview(target.getUserId(), furName, snippetOf(target), target.getCreatedDate()));
        }
        return result;
    }

    public MessageEntity persistSticker(String userId, String stickerImageUrl) {
        return persistSticker(userId, stickerImageUrl, null);
    }

    public MessageEntity persistSticker(String userId, String stickerImageUrl, String replyToMessageId) {
        if (stickerImageUrl == null || stickerImageUrl.isBlank()) {
            throw new IllegalArgumentException("sticker_image_url_required");
        }

        String normalizedReplyToMessageId = normalizeReplyToMessageId(replyToMessageId);

        return messageRepository.save(MessageEntity.builder()
                .messageId(messageIdGenerator.generate())
                .userId(userId)
                .messageType(MessageType.STICKER)
                .stickerImageUrl(stickerImageUrl.trim())
                .replyToMessageId(normalizedReplyToMessageId)
                .build());
    }

    /**
     * Host 軟刪除訊息。僅 host 可執行；非 host 擲 {@link ForbiddenException}，
     * 訊息不存在擲 {@link MessageNotFoundException}，已刪除則為 idempotent no-op。
     *
     * @return true 表示本次呼叫實際將訊息標記為刪除；false 表示先前已刪除（no-op）
     */
    public boolean softDelete(String messageId, String requesterProviderUserId) {
        if (!HostAuthz.isHost(requesterProviderUserId)) {
            throw new ForbiddenException();
        }

        MessageEntity message = messageRepository.findByMessageId(messageId)
                .orElseThrow(MessageNotFoundException::new);

        if (message.isDeleted()) {
            return false;
        }

        message.setDeleted(true);
        messageRepository.save(message);
        return true;
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

        if (entities.isEmpty()) {
            return List.of();
        }

        Map<String, AttendeeDataEntity> attendeeMap = attendeeDataRepository.findByUserIdIn(
                        entities.stream().map(MessageEntity::getUserId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(AttendeeDataEntity::getUserId, Function.identity()));

        Set<String> replyIds = entities.stream()
                .map(MessageEntity::getReplyToMessageId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, ReplyPreview> replyPreviews = resolveReplyPreviews(replyIds);

        return entities.stream()
                .map(entity -> MessageHistoryData.from(
                        entity,
                        attendeeMap.get(entity.getUserId()),
                        entity.getReplyToMessageId() == null ? null : replyPreviews.get(entity.getReplyToMessageId())))
                .toList();
    }
}
