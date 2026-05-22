package com.sef.cli.attendee.repository;

import com.sef.cli.attendee.entity.AttendeeStickerEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface AttendeeStickerRepository extends JpaRepository<AttendeeStickerEntity, Long> {

    List<AttendeeStickerEntity> findByUserIdOrderByStickerNo(String userId);

    Optional<AttendeeStickerEntity> findByUserIdAndStickerNo(String userId, Integer stickerNo);
}
