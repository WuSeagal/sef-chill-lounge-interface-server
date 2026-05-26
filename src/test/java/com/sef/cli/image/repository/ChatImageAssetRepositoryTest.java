package com.sef.cli.image.repository;

import com.sef.cli.config.JpaAuditingConfig;
import com.sef.cli.image.entity.ChatImageAssetEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class ChatImageAssetRepositoryTest {

    @Autowired
    private ChatImageAssetRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesAndAutoFillsUploadedDate() {
        ChatImageAssetEntity asset = ChatImageAssetEntity.builder()
                .fileName("test-260526143000-aaa.jpg")
                .uploadedBy("test-user-001")
                .fileSize(1024L)
                .build();

        ChatImageAssetEntity saved = repository.saveAndFlush(asset);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUploadedDate()).isNotNull();
    }

    @Test
    void rejectsDuplicateFileName() {
        repository.saveAndFlush(ChatImageAssetEntity.builder()
                .fileName("dup-260526143000-aaa.jpg")
                .uploadedBy("u").fileSize(1L).build());

        assertThatThrownBy(() -> repository.saveAndFlush(ChatImageAssetEntity.builder()
                .fileName("dup-260526143000-aaa.jpg")
                .uploadedBy("u").fileSize(1L).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsOldestNAscByUploadedDate() {
        repository.deleteAll();
        LocalDateTime base = LocalDateTime.of(2026, 5, 26, 10, 0);

        ChatImageAssetEntity a = repository.saveAndFlush(buildPlain("a.jpg"));
        ChatImageAssetEntity b = repository.saveAndFlush(buildPlain("b.jpg"));
        ChatImageAssetEntity c = repository.saveAndFlush(buildPlain("c.jpg"));

        overrideUploadedDate(a.getId(), base.minusDays(3));
        overrideUploadedDate(b.getId(), base.minusDays(1));
        overrideUploadedDate(c.getId(), base.minusDays(5));

        List<ChatImageAssetEntity> oldest = repository.findOldestN(2);

        assertThat(oldest).extracting(ChatImageAssetEntity::getFileName)
                .containsExactly("c.jpg", "a.jpg");
    }

    @Test
    void findsByCreatedBefore() {
        repository.deleteAll();
        LocalDateTime base = LocalDateTime.of(2026, 5, 26, 10, 0);

        ChatImageAssetEntity recent = repository.saveAndFlush(buildPlain("recent.jpg"));
        ChatImageAssetEntity old = repository.saveAndFlush(buildPlain("old.jpg"));

        overrideUploadedDate(recent.getId(), base.minusDays(1));
        overrideUploadedDate(old.getId(), base.minusDays(91));

        List<ChatImageAssetEntity> stale = repository.findByUploadedDateBefore(base.minusDays(90));

        assertThat(stale).extracting(ChatImageAssetEntity::getFileName).containsExactly("old.jpg");
    }

    private ChatImageAssetEntity buildPlain(String fileName) {
        return ChatImageAssetEntity.builder()
                .fileName(fileName).uploadedBy("u").fileSize(1L).build();
    }

    private void overrideUploadedDate(Long id, LocalDateTime when) {
        jdbcTemplate.update("update chat_image_assets set uploaded_date = ? where id = ?", when, id);
    }
}
