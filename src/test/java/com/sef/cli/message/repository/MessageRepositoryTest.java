package com.sef.cli.message.repository;

import com.sef.cli.config.JpaAuditingConfig;
import com.sef.cli.message.entity.MessageEntity;
import com.sef.cli.message.enums.MessageType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesTextMessageWithMultipleImages() {
        MessageEntity entity = MessageEntity.builder()
                .userId("message-test-001")
                .messageType(MessageType.TEXT)
                .content("hello with images")
                .imageUrls(List.of("/image/a.jpg", "/image/b.jpg"))
                .build();

        MessageEntity saved = messageRepository.saveAndFlush(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMessageId()).isNotBlank();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getImageUrls()).containsExactly("/image/a.jpg", "/image/b.jpg");
    }

    @Test
    void findsMessagesBeforeCursorOrderedByCreatedDateDescThenIdDesc() {
        messageRepository.deleteAll();
        LocalDateTime sharedTime = LocalDateTime.of(2026, 5, 25, 10, 0, 0);

        MessageEntity olderLowerId = messageRepository.saveAndFlush(MessageEntity.builder()
                .userId("message-test-002")
                .messageType(MessageType.TEXT)
                .content("older-lower-id")
                .build());

        MessageEntity olderHigherId = messageRepository.saveAndFlush(MessageEntity.builder()
                .userId("message-test-003")
                .messageType(MessageType.TEXT)
                .content("older-higher-id")
                .build());

        MessageEntity current = messageRepository.saveAndFlush(MessageEntity.builder()
                .userId("message-test-004")
                .messageType(MessageType.TEXT)
                .content("current")
                .build());

        jdbcTemplate.update("update messages set created_date = ? where id = ?", sharedTime, olderLowerId.getId());
        jdbcTemplate.update("update messages set created_date = ? where id = ?", sharedTime, olderHigherId.getId());
        jdbcTemplate.update("update messages set created_date = ? where id = ?", sharedTime.plusMinutes(1), current.getId());

        List<MessageEntity> result = messageRepository.findHistoryBefore(
                sharedTime.plusMinutes(1),
                current.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(result)
                .extracting(MessageEntity::getContent)
                .containsExactly("older-higher-id", "older-lower-id");
    }
}
