package com.sef.cli.image.repository;

import com.sef.cli.image.entity.ChatImageAssetEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatImageAssetRepository extends JpaRepository<ChatImageAssetEntity, Long> {

    @Query("SELECT c FROM ChatImageAssetEntity c ORDER BY c.uploadedDate ASC, c.id ASC")
    List<ChatImageAssetEntity> findOldestNQuery(PageRequest page);

    default List<ChatImageAssetEntity> findOldestN(int limit) {
        return findOldestNQuery(PageRequest.of(0, Math.max(1, limit)));
    }

    List<ChatImageAssetEntity> findByUploadedDateBefore(LocalDateTime threshold);
}
