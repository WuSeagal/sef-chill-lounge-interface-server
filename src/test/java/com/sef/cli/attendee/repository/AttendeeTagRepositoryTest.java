package com.sef.cli.attendee.repository;

import com.sef.cli.attendee.entity.AttendeeTagEntity;
import com.sef.cli.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AttendeeTagRepositoryTest {

    @Autowired
    private AttendeeTagRepository attendeeTagRepository;

    @Test
    void should_save_attendee_tag_relation() {
        AttendeeTagEntity entity = AttendeeTagEntity.builder()
                .userId("google-user-001")
                .tagId("tag-abc-123")
                .build();

        AttendeeTagEntity saved = attendeeTagRepository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
    }

    @Test
    void should_find_by_userId() {
        attendeeTagRepository.save(AttendeeTagEntity.builder()
                .userId("test-tag-user-001").tagId("tag-001").build());
        attendeeTagRepository.save(AttendeeTagEntity.builder()
                .userId("test-tag-user-001").tagId("tag-002").build());
        attendeeTagRepository.save(AttendeeTagEntity.builder()
                .userId("test-tag-user-999").tagId("tag-003").build());

        List<AttendeeTagEntity> tags = attendeeTagRepository.findByUserId("test-tag-user-001");

        assertThat(tags).hasSize(2);
    }

    @Test
    void should_delete_by_userId_and_tagId() {
        attendeeTagRepository.save(AttendeeTagEntity.builder()
                .userId("test-del-001").tagId("tag-del-001").build());
        attendeeTagRepository.save(AttendeeTagEntity.builder()
                .userId("test-del-001").tagId("tag-del-002").build());

        attendeeTagRepository.deleteByUserIdAndTagId("test-del-001", "tag-del-001");

        List<AttendeeTagEntity> remaining = attendeeTagRepository.findByUserId("test-del-001");
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getTagId()).isEqualTo("tag-del-002");
    }

    @Test
    void should_return_empty_for_unknown_userId() {
        List<AttendeeTagEntity> tags = attendeeTagRepository.findByUserId("nonexistent-tag-user");
        assertThat(tags).isEmpty();
    }

    @Test
    void should_reject_duplicate_userId_tagId_pair() {
        attendeeTagRepository.saveAndFlush(AttendeeTagEntity.builder()
                .userId("test-dup-001").tagId("tag-dup-001").build());

        AttendeeTagEntity duplicate = AttendeeTagEntity.builder()
                .userId("test-dup-001").tagId("tag-dup-001").build();

        assertThatThrownBy(() -> attendeeTagRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
