package com.sef.cli.attendee.repository;

import com.sef.cli.attendee.entity.AttendeeTagEntity;
import com.sef.cli.tag.entity.TagEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface AttendeeTagRepository extends JpaRepository<AttendeeTagEntity, Long> {

    List<AttendeeTagEntity> findByUserId(String userId);

    boolean existsByUserIdAndTagId(String userId, String tagId);

    Optional<AttendeeTagEntity> findByUserIdAndTagId(String userId, String tagId);

    void deleteByUserIdAndTagId(String userId, String tagId);

    long countByUserId(String userId);

    /** 用 userId 查所有關聯的 TagEntity（join junction）。 */
    @Query("SELECT t FROM TagEntity t WHERE t.tagId IN " +
            "(SELECT a.tagId FROM AttendeeTagEntity a WHERE a.userId = :userId)")
    List<TagEntity> findTagsByUserId(@Param("userId") String userId);

    /**
     * people-directory：單次批次取得所有 (userId, TagEntity) 配對，供 /members 一次組裝每位
     * attendee 的 tags，避免對每位 member 各打一次 tag 查詢（N+1）。每列 row[0]=userId(String)、
     * row[1]=TagEntity。
     */
    @Query("SELECT a.userId, t FROM AttendeeTagEntity a, TagEntity t WHERE a.tagId = t.tagId")
    List<Object[]> findAllUserIdTagPairs();
}
