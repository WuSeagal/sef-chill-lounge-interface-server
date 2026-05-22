package com.sef.cli.attendee.repository;

import com.sef.cli.attendee.entity.AttendeeStickerEntity;
import com.sef.cli.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AttendeeStickerRepositoryTest {

    @Autowired
    private AttendeeStickerRepository attendeeStickerRepository;

    @Test
    void should_save_sticker() {
        AttendeeStickerEntity entity = AttendeeStickerEntity.builder()
                .userId("test-sticker-001")
                .stickerNo(1)
                .sticker("/uploads/sticker/user001-1.gif")
                .build();

        AttendeeStickerEntity saved = attendeeStickerRepository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
    }

    @Test
    void should_find_by_userId_ordered_by_stickerNo() {
        attendeeStickerRepository.save(AttendeeStickerEntity.builder()
                .userId("test-sticker-order-001").stickerNo(3).sticker("/s3.gif").build());
        attendeeStickerRepository.save(AttendeeStickerEntity.builder()
                .userId("test-sticker-order-001").stickerNo(1).sticker("/s1.gif").build());
        attendeeStickerRepository.save(AttendeeStickerEntity.builder()
                .userId("test-sticker-order-001").stickerNo(2).sticker("/s2.gif").build());

        List<AttendeeStickerEntity> stickers = attendeeStickerRepository
                .findByUserIdOrderByStickerNo("test-sticker-order-001");

        assertThat(stickers).hasSize(3);
        assertThat(stickers.get(0).getStickerNo()).isEqualTo(1);
        assertThat(stickers.get(1).getStickerNo()).isEqualTo(2);
        assertThat(stickers.get(2).getStickerNo()).isEqualTo(3);
    }

    @Test
    void should_find_by_userId_and_stickerNo() {
        attendeeStickerRepository.save(AttendeeStickerEntity.builder()
                .userId("test-sticker-002").stickerNo(1).sticker("/s1.gif").build());
        attendeeStickerRepository.save(AttendeeStickerEntity.builder()
                .userId("test-sticker-002").stickerNo(2).sticker("/s2.gif").build());

        Optional<AttendeeStickerEntity> found = attendeeStickerRepository
                .findByUserIdAndStickerNo("test-sticker-002", 2);

        assertThat(found).isPresent();
        assertThat(found.get().getSticker()).isEqualTo("/s2.gif");
    }

    @Test
    void should_return_empty_for_unknown_stickerNo() {
        Optional<AttendeeStickerEntity> found = attendeeStickerRepository
                .findByUserIdAndStickerNo("test-sticker-002", 99);
        assertThat(found).isEmpty();
    }

    @Test
    void should_update_sticker_url() {
        AttendeeStickerEntity entity = AttendeeStickerEntity.builder()
                .userId("test-sticker-003")
                .stickerNo(1)
                .sticker("/old.gif")
                .build();
        AttendeeStickerEntity saved = attendeeStickerRepository.saveAndFlush(entity);

        saved.setSticker("/new.gif");
        AttendeeStickerEntity updated = attendeeStickerRepository.saveAndFlush(saved);

        assertThat(updated.getSticker()).isEqualTo("/new.gif");
    }

    @Test
    void should_reject_duplicate_userId_stickerNo_pair() {
        attendeeStickerRepository.saveAndFlush(AttendeeStickerEntity.builder()
                .userId("test-sticker-dup-001").stickerNo(1).sticker("/s1.gif").build());

        AttendeeStickerEntity duplicate = AttendeeStickerEntity.builder()
                .userId("test-sticker-dup-001").stickerNo(1).sticker("/s1-dup.gif").build();

        assertThatThrownBy(() -> attendeeStickerRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
