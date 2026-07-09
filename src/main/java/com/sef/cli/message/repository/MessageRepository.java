package com.sef.cli.message.repository;

import com.sef.cli.message.entity.MessageEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    // 名稱沿用既有歷史查詢，但以 @Query 排除 soft-deleted 訊息（deleted = false）。
    @Query("""
            select m
            from MessageEntity m
            where m.deleted = false
            order by m.createdDate desc, m.id desc
            """)
    List<MessageEntity> findAllByOrderByCreatedDateDescIdDesc(Pageable pageable);

    @Query("""
            select m
            from MessageEntity m
            where (m.createdDate < :before
               or (m.createdDate = :before and m.id < :beforeId))
              and m.deleted = false
            order by m.createdDate desc, m.id desc
            """)
    List<MessageEntity> findHistoryBefore(@Param("before") LocalDateTime before,
                                          @Param("beforeId") Long beforeId,
                                          Pageable pageable);

    // soft-delete 用：須能取得包含 deleted = true 的訊息（idempotent 判定）。
    Optional<MessageEntity> findByMessageId(String messageId);

    // 回覆預覽批次解析用：排除已軟刪除的目標，讓「原訊息之後才被刪除」的舊回覆能查無結果。
    List<MessageEntity> findByMessageIdInAndDeletedFalse(Collection<String> messageIds);
}
