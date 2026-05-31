package com.sef.cli.message.service;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.message.entity.MessageEntity;
import com.sef.cli.message.enums.MessageType;
import com.sef.cli.message.repository.MessageRepository;
import com.sef.cli.message.service.dto.MessageHistoryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AttendeeDataRepository attendeeDataRepository;

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
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageEntity saved = messageService.persistSticker("u-1", "/sticker/u-1/1.png?v=1");

        assertThat(saved.getMessageType()).isEqualTo(MessageType.STICKER);
        assertThat(saved.getStickerImageUrl()).isEqualTo("/sticker/u-1/1.png?v=1");
    }

    @Test
    void persistStickerRejectsBlankStickerImageUrl() {
        assertThatThrownBy(() -> messageService.persistSticker("u-1", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sticker_image_url_required");
    }

    @Test
    void persistTextStoresNormalizedPayload() {
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> {
            MessageEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            entity.setMessageId("msg-001");
            entity.setCreatedDate(LocalDateTime.of(2026, 5, 25, 10, 0, 0));
            return entity;
        });

        MessageEntity saved = messageService.persistText("google-001", " hello ",
                List.of("/image/a-260526143000-aaa.jpg", "/image/b-260526143000-bbb.jpg"));

        assertThat(saved.getMessageType()).isEqualTo(MessageType.TEXT);
        assertThat(saved.getContent()).isEqualTo("hello");
        assertThat(saved.getImageUrls()).containsExactly(
                "/image/a-260526143000-aaa.jpg", "/image/b-260526143000-bbb.jpg");
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
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        MessageEntity result = messageService.persistText(
                "u-1", "hello", List.of("/image/u1abcd-260526143000-x7K.png"));
        assertThat(result.getImageUrls()).containsExactly("/image/u1abcd-260526143000-x7K.png");
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
        when(attendeeDataRepository.findByUserIdIn(Set.of())).thenReturn(List.of());

        messageService.loadHistory(null, null, 999);

        verify(messageRepository).findAllByOrderByCreatedDateDescIdDesc(
                argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 100)
        );
    }
}
