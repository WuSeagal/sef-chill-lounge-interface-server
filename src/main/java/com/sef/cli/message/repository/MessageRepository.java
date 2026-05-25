package com.sef.cli.message.repository;

import com.sef.cli.message.entity.MessageEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    List<MessageEntity> findAllByOrderByCreatedDateDescIdDesc(Pageable pageable);

    @Query("""
            select m
            from MessageEntity m
            where m.createdDate < :before
               or (m.createdDate = :before and m.id < :beforeId)
            order by m.createdDate desc, m.id desc
            """)
    List<MessageEntity> findHistoryBefore(@Param("before") LocalDateTime before,
                                          @Param("beforeId") Long beforeId,
                                          Pageable pageable);
}
