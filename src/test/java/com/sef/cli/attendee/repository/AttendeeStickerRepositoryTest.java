package com.sef.cli.attendee.repository;

import com.sef.cli.attendee.entity.AttendeeStickerEntity;
import com.sef.cli.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AttendeeStickerRepositoryTest {

    @Autowired
    private AttendeeStickerRepository attendeeStickerRepository;

    @Test
    void findByUserIdOrderByCreatedDateAsc_returnsAllRowsForUser() {
        String userId = "test-sticker-dyn-001";
        attendeeStickerRepository.save(AttendeeStickerEntity.builder().userId(userId).sticker("/s1.gif").build());
        attendeeStickerRepository.save(AttendeeStickerEntity.builder().userId(userId).sticker("/s2.gif").build());
        attendeeStickerRepository.save(AttendeeStickerEntity.builder().userId(userId).sticker("/s3.gif").build());

        List<AttendeeStickerEntity> stickers = attendeeStickerRepository
                .findByUserIdOrderByCreatedDateAsc(userId);

        assertThat(stickers).hasSize(3);
        assertThat(stickers).allMatch(e -> userId.equals(e.getUserId()));
    }

    @Test
    void countByUserId_returnsCorrectCount() {
        String userId = "test-sticker-dyn-002";
        attendeeStickerRepository.save(AttendeeStickerEntity.builder().userId(userId).sticker("/s1.gif").build());
        attendeeStickerRepository.save(AttendeeStickerEntity.builder().userId(userId).sticker("/s2.gif").build());
        attendeeStickerRepository.save(AttendeeStickerEntity.builder().userId(userId).sticker("/s3.gif").build());

        long count = attendeeStickerRepository.countByUserId(userId);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void findByIdAndUserId_returnsRowForCorrectUser() {
        String userId = "test-sticker-dyn-003";
        AttendeeStickerEntity saved = attendeeStickerRepository.save(
                AttendeeStickerEntity.builder().userId(userId).sticker("/s1.gif").build());

        Optional<AttendeeStickerEntity> found = attendeeStickerRepository
                .findByIdAndUserId(saved.getId(), userId);

        assertThat(found).isPresent();
        assertThat(found.get().getSticker()).isEqualTo("/s1.gif");
    }

    @Test
    void findByIdAndUserId_returnsEmptyForWrongUser() {
        String userId = "test-sticker-dyn-004";
        AttendeeStickerEntity saved = attendeeStickerRepository.save(
                AttendeeStickerEntity.builder().userId(userId).sticker("/s1.gif").build());

        Optional<AttendeeStickerEntity> found = attendeeStickerRepository
                .findByIdAndUserId(saved.getId(), "wrong-user");

        assertThat(found).isEmpty();
    }
}
