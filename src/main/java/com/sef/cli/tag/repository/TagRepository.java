package com.sef.cli.tag.repository;

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
public interface TagRepository extends JpaRepository<TagEntity, Long> {

    Optional<TagEntity> findByTagId(String tagId);

    List<TagEntity> findByType(String type);

    List<TagEntity> findByTypeNot(String type);

    @Query("""
            SELECT t FROM TagEntity t
            WHERE t.isCustom = false
               OR t.tagId IN (
                   SELECT at.tagId FROM AttendeeTagEntity at
                   GROUP BY at.tagId
                   HAVING COUNT(at) >= :threshold
               )
            """)
    List<TagEntity> findSelectableTags(@Param("threshold") int threshold);
}
