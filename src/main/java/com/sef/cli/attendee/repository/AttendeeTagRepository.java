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

    /** 用 userId 查所有關聯的 TagEntity（join junction）。 */
    @Query("SELECT t FROM TagEntity t WHERE t.tagId IN " +
            "(SELECT a.tagId FROM AttendeeTagEntity a WHERE a.userId = :userId)")
    List<TagEntity> findTagsByUserId(@Param("userId") String userId);
}
