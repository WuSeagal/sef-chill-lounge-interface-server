package com.sef.cli.attendee.repository;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AttendeeDataRepositoryTest {

    @Autowired
    private AttendeeDataRepository attendeeDataRepository;

    @Test
    void should_save_and_find_by_id() {
        AttendeeDataEntity entity = AttendeeDataEntity.builder()
                .userId("google-user-001")
                .username("testuser")
                .furName("TestFur")
                .avatar("/uploads/avatar/test.png")
                .avatarColor("#FF5733")
                .topicId("topic-abc")
                .build();

        AttendeeDataEntity saved = attendeeDataRepository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getLastModifiedDate()).isNotNull();
    }

    @Test
    void should_find_by_userId() {
        AttendeeDataEntity entity = AttendeeDataEntity.builder()
                .userId("google-user-002")
                .username("user2")
                .furName("Fur2")
                .build();
        attendeeDataRepository.save(entity);

        Optional<AttendeeDataEntity> found = attendeeDataRepository.findByUserId("google-user-002");

        assertThat(found).isPresent();
        assertThat(found.get().getFurName()).isEqualTo("Fur2");
    }

    @Test
    void should_return_empty_for_unknown_userId() {
        Optional<AttendeeDataEntity> found = attendeeDataRepository.findByUserId("nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    void should_reject_duplicate_userId() {
        AttendeeDataEntity first = AttendeeDataEntity.builder()
                .userId("google-dup-001")
                .username("user1")
                .furName("Fur1")
                .build();
        attendeeDataRepository.saveAndFlush(first);

        AttendeeDataEntity duplicate = AttendeeDataEntity.builder()
                .userId("google-dup-001")
                .username("user1dup")
                .furName("Fur1Dup")
                .build();

        assertThatThrownBy(() -> attendeeDataRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_update_and_refresh_lastModifiedDate() {
        AttendeeDataEntity entity = AttendeeDataEntity.builder()
                .userId("google-user-003")
                .username("user3")
                .furName("OldName")
                .build();
        AttendeeDataEntity saved = attendeeDataRepository.saveAndFlush(entity);

        saved.setFurName("NewName");
        AttendeeDataEntity updated = attendeeDataRepository.saveAndFlush(saved);

        assertThat(updated.getFurName()).isEqualTo("NewName");
        assertThat(updated.getLastModifiedDate()).isNotNull();
    }

    @Test
    void should_find_all_attendees() {
        long initialCount = attendeeDataRepository.count();

        attendeeDataRepository.save(AttendeeDataEntity.builder()
                .userId("google-all-001").username("u1").furName("F1").build());
        attendeeDataRepository.save(AttendeeDataEntity.builder()
                .userId("google-all-002").username("u2").furName("F2").build());

        assertThat(attendeeDataRepository.findAll()).hasSize((int) initialCount + 2);
    }
}
