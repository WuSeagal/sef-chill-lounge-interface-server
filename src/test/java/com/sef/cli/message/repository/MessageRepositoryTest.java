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
                .messageId("260612130001AbCd1234")
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
    void savesMessageWithReplyToMessageIdRoundTrip() {
        MessageEntity entity = MessageEntity.builder()
                .messageId("260707130020AbCd1234")
                .userId("reply-test-001")
                .messageType(MessageType.TEXT)
                .content("好可愛")
                .replyToMessageId("260707130001AbCd1234")
                .build();

        MessageEntity saved = messageRepository.saveAndFlush(entity);

        assertThat(saved.getReplyToMessageId()).isEqualTo("260707130001AbCd1234");
    }

    @Test
    void nonReplyMessageHasNullReplyToMessageId() {
        MessageEntity entity = MessageEntity.builder()
                .messageId("260707130021AbCd1234")
                .userId("reply-test-002")
                .messageType(MessageType.TEXT)
                .content("一般訊息")
                .build();

        MessageEntity saved = messageRepository.saveAndFlush(entity);

        assertThat(saved.getReplyToMessageId()).isNull();
    }

    @Test
    void findsMessagesBeforeCursorOrderedByCreatedDateDescThenIdDesc() {
        messageRepository.deleteAll();
        LocalDateTime sharedTime = LocalDateTime.of(2026, 5, 25, 10, 0, 0);

        MessageEntity olderLowerId = messageRepository.saveAndFlush(MessageEntity.builder()
                .messageId("260612130002AbCd1234")
                .userId("message-test-002")
                .messageType(MessageType.TEXT)
                .content("older-lower-id")
                .build());

        MessageEntity olderHigherId = messageRepository.saveAndFlush(MessageEntity.builder()
                .messageId("260612130003AbCd1234")
                .userId("message-test-003")
                .messageType(MessageType.TEXT)
                .content("older-higher-id")
                .build());

        MessageEntity current = messageRepository.saveAndFlush(MessageEntity.builder()
                .messageId("260612130004AbCd1234")
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

    @Test
    void seedMessageIdsShouldUseTimestampedFormat() {
        List<String> messageIds = jdbcTemplate.queryForList("select message_id from messages order by id", String.class);

        assertThat(messageIds).allMatch(id -> id.matches("\\d{12}[0-9A-Za-z_-]{8}"));
    }

    @Test
    void findByMessageIdReturnsMessageEvenWhenDeleted() {
        messageRepository.deleteAll();
        MessageEntity entity = messageRepository.saveAndFlush(MessageEntity.builder()
                .messageId("260612130010AbCd1234")
                .userId("del-test-001")
                .messageType(MessageType.TEXT)
                .content("soft deleted")
                .build());
        jdbcTemplate.update("update messages set deleted = true where id = ?", entity.getId());

        assertThat(messageRepository.findByMessageId("260612130010AbCd1234")).isPresent();
    }

    @Test
    void findAllExcludesDeletedMessages() {
        messageRepository.deleteAll();
        messageRepository.saveAndFlush(MessageEntity.builder()
                .messageId("260612130011AbCd1234")
                .userId("del-test-002")
                .messageType(MessageType.TEXT)
                .content("visible")
                .build());
        MessageEntity deleted = messageRepository.saveAndFlush(MessageEntity.builder()
                .messageId("260612130012AbCd1234")
                .userId("del-test-003")
                .messageType(MessageType.TEXT)
                .content("deleted")
                .build());
        jdbcTemplate.update("update messages set deleted = true where id = ?", deleted.getId());

        List<MessageEntity> result = messageRepository.findAllByOrderByCreatedDateDescIdDesc(PageRequest.of(0, 10));

        assertThat(result).extracting(MessageEntity::getContent).containsExactly("visible");
    }

    @Test
    void findHistoryBeforeExcludesDeletedMessages() {
        messageRepository.deleteAll();
        LocalDateTime sharedTime = LocalDateTime.of(2026, 5, 25, 10, 0, 0);

        MessageEntity olderVisible = messageRepository.saveAndFlush(MessageEntity.builder()
                .messageId("260612130013AbCd1234")
                .userId("del-test-004")
                .messageType(MessageType.TEXT)
                .content("older-visible")
                .build());
        MessageEntity olderDeleted = messageRepository.saveAndFlush(MessageEntity.builder()
                .messageId("260612130014AbCd1234")
                .userId("del-test-005")
                .messageType(MessageType.TEXT)
                .content("older-deleted")
                .build());
        MessageEntity current = messageRepository.saveAndFlush(MessageEntity.builder()
                .messageId("260612130015AbCd1234")
                .userId("del-test-006")
                .messageType(MessageType.TEXT)
                .content("current")
                .build());

        jdbcTemplate.update("update messages set created_date = ? where id = ?", sharedTime, olderVisible.getId());
        jdbcTemplate.update("update messages set created_date = ? where id = ?", sharedTime, olderDeleted.getId());
        jdbcTemplate.update("update messages set created_date = ? where id = ?", sharedTime.plusMinutes(1), current.getId());
        jdbcTemplate.update("update messages set deleted = true where id = ?", olderDeleted.getId());

        List<MessageEntity> result = messageRepository.findHistoryBefore(
                sharedTime.plusMinutes(1),
                current.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(result)
                .extracting(MessageEntity::getContent)
                .containsExactly("older-visible");
    }

    @Test
    void findByMessageIdInAndDeletedFalseExcludesDeletedTargets() {
        messageRepository.deleteAll();
        MessageEntity visible = messageRepository.saveAndFlush(MessageEntity.builder()
                .messageId("260707130030AbCd1234")
                .userId("target-test-001")
                .messageType(MessageType.TEXT)
                .content("visible-target")
                .build());
        MessageEntity deletedTarget = messageRepository.saveAndFlush(MessageEntity.builder()
                .messageId("260707130031AbCd1234")
                .userId("target-test-002")
                .messageType(MessageType.TEXT)
                .content("deleted-target")
                .build());
        jdbcTemplate.update("update messages set deleted = true where id = ?", deletedTarget.getId());

        List<MessageEntity> result = messageRepository.findByMessageIdInAndDeletedFalse(
                List.of(visible.getMessageId(), deletedTarget.getMessageId()));

        assertThat(result).extracting(MessageEntity::getMessageId).containsExactly(visible.getMessageId());
    }

    @Test
    void findByMessageIdInAndDeletedFalseIgnoresNonExistentIds() {
        messageRepository.deleteAll();
        MessageEntity visible = messageRepository.saveAndFlush(MessageEntity.builder()
                .messageId("260707130032AbCd1234")
                .userId("target-test-003")
                .messageType(MessageType.TEXT)
                .content("visible-target-2")
                .build());

        List<MessageEntity> result = messageRepository.findByMessageIdInAndDeletedFalse(
                List.of(visible.getMessageId(), "does-not-exist"));

        assertThat(result).extracting(MessageEntity::getMessageId).containsExactly(visible.getMessageId());
    }
}
