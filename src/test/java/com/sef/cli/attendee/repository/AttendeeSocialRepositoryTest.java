package com.sef.cli.attendee.repository;

import com.sef.cli.attendee.entity.AttendeeSocialEntity;
import com.sef.cli.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AttendeeSocialRepositoryTest {

    @Autowired
    private AttendeeSocialRepository attendeeSocialRepository;

    @Test
    void should_save_social_link() {
        AttendeeSocialEntity entity = AttendeeSocialEntity.builder()
                .userId("google-user-001")
                .platform("twitter")
                .links("https://twitter.com/testuser")
                .build();

        AttendeeSocialEntity saved = attendeeSocialRepository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getLastModifiedDate()).isNotNull();
    }

    @Test
    void should_find_by_userId() {
        attendeeSocialRepository.save(AttendeeSocialEntity.builder()
                .userId("test-social-001").platform("twitter").links("https://twitter.com/u1").build());
        attendeeSocialRepository.save(AttendeeSocialEntity.builder()
                .userId("test-social-001").platform("plurk").links("https://plurk.com/u1").build());
        attendeeSocialRepository.save(AttendeeSocialEntity.builder()
                .userId("test-social-999").platform("twitter").links("https://twitter.com/u9").build());

        List<AttendeeSocialEntity> socials = attendeeSocialRepository.findByUserId("test-social-001");

        assertThat(socials).hasSize(2);
    }

    @Test
    void should_find_by_userId_and_platform() {
        attendeeSocialRepository.save(AttendeeSocialEntity.builder()
                .userId("test-social-002").platform("twitter").links("https://twitter.com/u2").build());
        attendeeSocialRepository.save(AttendeeSocialEntity.builder()
                .userId("test-social-002").platform("plurk").links("https://plurk.com/u2").build());

        List<AttendeeSocialEntity> found = attendeeSocialRepository
                .findByUserIdAndPlatform("test-social-002", "twitter");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getLinks()).isEqualTo("https://twitter.com/u2");
    }

    @Test
    void should_update_links() {
        AttendeeSocialEntity entity = AttendeeSocialEntity.builder()
                .userId("test-social-003")
                .platform("twitter")
                .links("https://twitter.com/old")
                .build();
        AttendeeSocialEntity saved = attendeeSocialRepository.saveAndFlush(entity);

        saved.setLinks("https://twitter.com/new");
        AttendeeSocialEntity updated = attendeeSocialRepository.saveAndFlush(saved);

        assertThat(updated.getLinks()).isEqualTo("https://twitter.com/new");
    }

    @Test
    void should_delete_social_link() {
        AttendeeSocialEntity entity = attendeeSocialRepository.save(AttendeeSocialEntity.builder()
                .userId("test-social-004").platform("twitter").links("https://twitter.com/u4").build());

        attendeeSocialRepository.deleteById(entity.getId());

        assertThat(attendeeSocialRepository.findByUserId("test-social-004")).isEmpty();
    }
}
