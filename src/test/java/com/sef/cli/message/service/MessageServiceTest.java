package com.sef.cli.message.service;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.message.entity.MessageEntity;
import com.sef.cli.message.enums.MessageType;
import com.sef.cli.common.HostAuthz;
import com.sef.cli.common.exception.ForbiddenException;
import com.sef.cli.common.exception.MessageNotFoundException;
import com.sef.cli.message.repository.MessageRepository;
import com.sef.cli.message.service.dto.MessageHistoryData;
import com.sef.cli.message.service.dto.ReplyPreview;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AttendeeDataRepository attendeeDataRepository;

    @Mock
    private MessageIdGenerator messageIdGenerator;

    @InjectMocks
    private MessageService messageService;

    @Test
    void persistTextRejectsBlankContentAndEmptyImages() {
        assertThatThrownBy(() -> messageService.persistText("u-1", "   ", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message_content_required");
    }

    @Test
    void persistTextRejectsMoreThanFiveImages() {
        assertThatThrownBy(() -> messageService.persistText("u-1", "hello", List.of("1", "2", "3", "4", "5", "6")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message_images_limit_exceeded");
    }

    @Test
    void persistTextRejectsContentLongerThan500Chars() {
        String tooLong = "a".repeat(501);
        assertThatThrownBy(() -> messageService.persistText("u-1", tooLong, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message_content_too_long");
    }

    @Test
    void persistStickerStoresStickerMessage() {
        when(messageIdGenerator.generate()).thenReturn("260612153045AbC123xy");
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageEntity saved = messageService.persistSticker("u-1", "/sticker/u-1/1.png?v=1");

        assertThat(saved.getMessageType()).isEqualTo(MessageType.STICKER);
        assertThat(saved.getStickerImageUrl()).isEqualTo("/sticker/u-1/1.png?v=1");
        assertThat(saved.getMessageId()).isEqualTo("260612153045AbC123xy");
    }

    @Test
    void persistStickerRejectsBlankStickerImageUrl() {
        assertThatThrownBy(() -> messageService.persistSticker("u-1", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sticker_image_url_required");
    }

    @Test
    void persistStickerStoresReplyToMessageIdVerbatimWithoutAnyLookup() {
        when(messageIdGenerator.generate()).thenReturn("260612153045AbC123xy");
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageEntity saved = messageService.persistSticker("u-1", "/sticker/u-1/1.png?v=1", "target-msg");

        assertThat(saved.getMessageType()).isEqualTo(MessageType.STICKER);
        assertThat(saved.getReplyToMessageId()).isEqualTo("target-msg");
        verify(messageRepository, never()).findByMessageId(any());
    }

    @Test
    void persistStickerWithoutReplyLeavesReplyToMessageIdNull() {
        when(messageIdGenerator.generate()).thenReturn("260612153045AbC123xy");
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageEntity saved = messageService.persistSticker("u-1", "/sticker/u-1/1.png?v=1", null);

        assertThat(saved.getReplyToMessageId()).isNull();
    }

    @Test
    void persistTextRejectsReplyToMessageIdLongerThan64Chars() {
        String tooLong = "a".repeat(65);
        assertThatThrownBy(() -> messageService.persistText("u-1", "hello", List.of(), tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("reply_to_message_id_invalid");
    }

    @Test
    void persistStickerRejectsReplyToMessageIdLongerThan64Chars() {
        String tooLong = "a".repeat(65);
        assertThatThrownBy(() -> messageService.persistSticker("u-1", "/sticker/u-1/1.png", tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("reply_to_message_id_invalid");
    }

    @Test
    void persistTextStoresNormalizedPayload() {
        when(messageIdGenerator.generate()).thenReturn("260612153045AbC123xy");
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> {
            MessageEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            entity.setCreatedDate(LocalDateTime.of(2026, 5, 25, 10, 0, 0));
            return entity;
        });

        MessageEntity saved = messageService.persistText("google-001", " hello ",
                List.of("/image/a-260526143000-aaa.jpg", "/image/b-260526143000-bbb.jpg"));

        assertThat(saved.getMessageType()).isEqualTo(MessageType.TEXT);
        assertThat(saved.getMessageId()).isEqualTo("260612153045AbC123xy");
        assertThat(saved.getContent()).isEqualTo("hello");
        assertThat(saved.getImageUrls()).containsExactly(
                "/image/a-260526143000-aaa.jpg", "/image/b-260526143000-bbb.jpg");
    }

    @Test
    void persistTextStoresReplyToMessageIdVerbatimWithoutAnyLookup() {
        when(messageIdGenerator.generate()).thenReturn("260707153045AbC123xy");
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageEntity saved = messageService.persistText("google-001", "好可愛", List.of(), "target-msg");

        assertThat(saved.getReplyToMessageId()).isEqualTo("target-msg");
        // persistText 本身不查詢目標訊息或作者資料——解析交給 resolveReplyPreview，各自獨立測試。
        verify(messageRepository, never()).findByMessageId(any());
        verify(attendeeDataRepository, never()).findByUserId(any());
    }

    @Test
    void persistTextTrimsBlankReplyToMessageIdToNull() {
        when(messageIdGenerator.generate()).thenReturn("260707153045AbC123xy");
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageEntity saved = messageService.persistText("google-001", "一般", List.of(), "   ");

        assertThat(saved.getReplyToMessageId()).isNull();
    }

    @Test
    void persistTextWithoutReplyLeavesReplyToMessageIdNull() {
        when(messageIdGenerator.generate()).thenReturn("260707153045AbC123xy");
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageEntity saved = messageService.persistText("google-001", "一般訊息", List.of(), null);

        assertThat(saved.getReplyToMessageId()).isNull();
    }

    @Test
    void persistTextRejectsImageUrlNotStartingWithImagePrefix() {
        assertThatThrownBy(() -> messageService.persistText(
                "u-1", "hello", List.of("/foreign/cdn.example.com/x.jpg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message_image_url_invalid_prefix");
    }

    @Test
    void persistTextAcceptsImageUrlWithImagePrefix() {
        when(messageIdGenerator.generate()).thenReturn("260612153045AbC123xy");
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        MessageEntity result = messageService.persistText(
                "u-1", "hello", List.of("/image/u1abcd-260526143000-x7K.png"));
        assertThat(result.getImageUrls()).containsExactly("/image/u1abcd-260526143000-x7K.png");
        assertThat(result.getMessageId()).isEqualTo("260612153045AbC123xy");
    }

    @Test
    void loadHistoryHydratesLatestFurNameAndAvatar() {
        MessageEntity entity = MessageEntity.builder()
                .id(11L)
                .messageId("msg-001")
                .userId("google-001")
                .messageType(MessageType.TEXT)
                .content("hello")
                .createdDate(LocalDateTime.of(2026, 5, 25, 10, 0, 0))
                .build();

        when(messageRepository.findAllByOrderByCreatedDateDescIdDesc(any())).thenReturn(List.of(entity));
        when(attendeeDataRepository.findByUserIdIn(Set.of("google-001"))).thenReturn(List.of(
                AttendeeDataEntity.builder()
                        .userId("google-001")
                        .furName("Fox")
                        .avatar("/avatar.png")
                        .build()
        ));

        List<MessageHistoryData> result = messageService.loadHistory(null, null, 50);

        assertThat(result).singleElement().satisfies(message -> {
            assertThat(message.cursorId()).isEqualTo(11L);
            assertThat(message.furName()).isEqualTo("Fox");
            assertThat(message.avatar()).isEqualTo("/avatar.png");
            assertThat(message.content()).isEqualTo("hello");
        });
    }

    @Test
    void loadHistoryCapsLimitAt100() {
        when(messageRepository.findAllByOrderByCreatedDateDescIdDesc(any())).thenReturn(List.of());

        messageService.loadHistory(null, null, 999);

        verify(messageRepository).findAllByOrderByCreatedDateDescIdDesc(
                argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 100)
        );
    }

    @Test
    void loadHistoryDoesNotQueryAttendeesWhenNoMessages() {
        when(messageRepository.findAllByOrderByCreatedDateDescIdDesc(any())).thenReturn(List.of());

        List<MessageHistoryData> result = messageService.loadHistory(null, null, 50);

        assertThat(result).isEmpty();
        verify(attendeeDataRepository, never()).findByUserIdIn(any());
    }

    @Test
    void resolveReplyPreviewReturnsTargetAuthorAndSnippet() {
        LocalDateTime targetTime = LocalDateTime.of(2026, 5, 20, 14, 2, 15);
        MessageEntity target = MessageEntity.builder()
                .id(5L).messageId("target-msg").userId("google-target")
                .messageType(MessageType.TEXT).content("看看這張").createdDate(targetTime).build();
        when(messageRepository.findByMessageIdInAndDeletedFalse(Set.of("target-msg"))).thenReturn(List.of(target));
        when(attendeeDataRepository.findByUserIdIn(Set.of("google-target"))).thenReturn(List.of(
                AttendeeDataEntity.builder().userId("google-target").furName("小白").build()));

        ReplyPreview preview = messageService.resolveReplyPreview("target-msg");

        assertThat(preview).isNotNull();
        assertThat(preview.targetUserId()).isEqualTo("google-target");
        assertThat(preview.furName()).isEqualTo("小白");
        assertThat(preview.contentSnippet()).isEqualTo("看看這張");
        assertThat(preview.createdDate()).isEqualTo(targetTime);
    }

    @Test
    void resolveReplyPreviewReturnsNullWhenTargetNotFoundOrDeleted() {
        when(messageRepository.findByMessageIdInAndDeletedFalse(Set.of("gone"))).thenReturn(List.of());

        ReplyPreview preview = messageService.resolveReplyPreview("gone");

        assertThat(preview).isNull();
    }

    @Test
    void resolveReplyPreviewReturnsNullForBlankId() {
        ReplyPreview preview = messageService.resolveReplyPreview("  ");

        assertThat(preview).isNull();
        verify(messageRepository, never()).findByMessageIdInAndDeletedFalse(any());
    }

    @Test
    void resolveReplyPreviewsBatchResolvesAuthorNotAmongCallerKnownUserIds() {
        // 防 union 遺漏：目標作者「google-target」不在呼叫端事先已知的任何 userId 集合內
        // （模擬「回覆一則很久以前、作者不在本頁發送者之列的訊息」）。
        LocalDateTime t1 = LocalDateTime.of(2026, 5, 1, 9, 0, 0);
        MessageEntity target = MessageEntity.builder()
                .id(1L).messageId("old-msg").userId("google-target")
                .messageType(MessageType.TEXT).content("很久以前的訊息").createdDate(t1).build();
        when(messageRepository.findByMessageIdInAndDeletedFalse(Set.of("old-msg"))).thenReturn(List.of(target));
        when(attendeeDataRepository.findByUserIdIn(Set.of("google-target"))).thenReturn(List.of(
                AttendeeDataEntity.builder().userId("google-target").furName("老王").build()));

        Map<String, ReplyPreview> result = messageService.resolveReplyPreviews(Set.of("old-msg"));

        assertThat(result).containsKey("old-msg");
        assertThat(result.get("old-msg").furName()).isEqualTo("老王");
    }

    @Test
    void resolveReplyPreviewsReturnsEmptyMapWithoutQueryingWhenIdsEmpty() {
        Map<String, ReplyPreview> result = messageService.resolveReplyPreviews(Set.of());

        assertThat(result).isEmpty();
        verify(messageRepository, never()).findByMessageIdInAndDeletedFalse(any());
    }

    @Test
    void loadHistoryHydratesReplyPreviewReflectingRenameAfterSend() {
        // 證明此次翻轉的關鍵測試：目標作者送出後改名，loadHistory 讀到的應是新名字，非送出當下的舊名字。
        LocalDateTime targetTime = LocalDateTime.of(2026, 5, 20, 14, 2, 15);
        MessageEntity target = MessageEntity.builder()
                .id(5L).messageId("target-msg").userId("google-target")
                .messageType(MessageType.TEXT).content("看看這張").createdDate(targetTime).build();
        MessageEntity reply = MessageEntity.builder()
                .id(11L).messageId("reply-msg").userId("google-001")
                .messageType(MessageType.TEXT).content("好可愛")
                .createdDate(LocalDateTime.of(2026, 5, 20, 14, 5, 0))
                .replyToMessageId("target-msg").build();

        when(messageRepository.findAllByOrderByCreatedDateDescIdDesc(any())).thenReturn(List.of(reply));
        when(messageRepository.findByMessageIdInAndDeletedFalse(Set.of("target-msg"))).thenReturn(List.of(target));
        when(attendeeDataRepository.findByUserIdIn(Set.of("google-001"))).thenReturn(List.of(
                AttendeeDataEntity.builder().userId("google-001").furName("Fox").build()));
        when(attendeeDataRepository.findByUserIdIn(Set.of("google-target"))).thenReturn(List.of(
                AttendeeDataEntity.builder().userId("google-target").furName("小白兔").build())); // 已改名

        List<MessageHistoryData> result = messageService.loadHistory(null, null, 50);

        assertThat(result).singleElement().satisfies(message -> {
            assertThat(message.replyToUserId()).isEqualTo("google-target");
            assertThat(message.replyToFurName()).isEqualTo("小白兔");
            assertThat(message.replyToContentSnippet()).isEqualTo("看看這張");
            assertThat(message.replyToCreatedDate()).isEqualTo(targetTime);
        });
    }

    @Test
    void loadHistoryExcludesReplyPreviewForTargetDeletedAfterSend() {
        MessageEntity reply = MessageEntity.builder()
                .id(11L).messageId("reply-msg").userId("google-001")
                .messageType(MessageType.TEXT).content("好可愛")
                .createdDate(LocalDateTime.of(2026, 5, 20, 14, 5, 0))
                .replyToMessageId("target-msg").build();

        when(messageRepository.findAllByOrderByCreatedDateDescIdDesc(any())).thenReturn(List.of(reply));
        // 目標之後被軟刪 → findByMessageIdInAndDeletedFalse 查無結果
        when(messageRepository.findByMessageIdInAndDeletedFalse(Set.of("target-msg"))).thenReturn(List.of());
        when(attendeeDataRepository.findByUserIdIn(Set.of("google-001"))).thenReturn(List.of(
                AttendeeDataEntity.builder().userId("google-001").furName("Fox").build()));

        List<MessageHistoryData> result = messageService.loadHistory(null, null, 50);

        assertThat(result).singleElement().satisfies(message -> {
            assertThat(message.replyToMessageId()).isEqualTo("target-msg");
            assertThat(message.replyToUserId()).isNull();
            assertThat(message.replyToFurName()).isNull();
            assertThat(message.replyToContentSnippet()).isNull();
            assertThat(message.replyToCreatedDate()).isNull();
        });
    }

    @Test
    void softDeleteByHostMarksDeletedAndReturnsTrue() {
        MessageEntity msg = MessageEntity.builder()
                .id(11L)
                .messageId("m-1")
                .userId("author-001")
                .messageType(MessageType.TEXT)
                .content("inappropriate")
                .build();
        when(messageRepository.findByMessageId("m-1")).thenReturn(Optional.of(msg));
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean changed = messageService.softDelete("m-1", HostAuthz.HOST_PROVIDER_USER_ID);

        assertThat(changed).isTrue();
        assertThat(msg.isDeleted()).isTrue();
        verify(messageRepository).save(msg);
    }

    @Test
    void softDeleteByNonHostThrowsForbiddenAndDoesNotTouchRepository() {
        assertThatThrownBy(() -> messageService.softDelete("m-1", "not-the-host"))
                .isInstanceOf(ForbiddenException.class);
        verify(messageRepository, never()).findByMessageId(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void softDeleteMissingMessageThrowsNotFound() {
        when(messageRepository.findByMessageId("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.softDelete("nope", HostAuthz.HOST_PROVIDER_USER_ID))
                .isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    void softDeleteAlreadyDeletedIsIdempotentNoOp() {
        MessageEntity msg = MessageEntity.builder()
                .id(11L)
                .messageId("m-1")
                .userId("author-001")
                .messageType(MessageType.TEXT)
                .content("already gone")
                .deleted(true)
                .build();
        when(messageRepository.findByMessageId("m-1")).thenReturn(Optional.of(msg));

        boolean changed = messageService.softDelete("m-1", HostAuthz.HOST_PROVIDER_USER_ID);

        assertThat(changed).isFalse();
        verify(messageRepository, never()).save(any());
    }
}
