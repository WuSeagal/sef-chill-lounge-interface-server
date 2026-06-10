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

    /**
     * 依 type + content 查同內容 TAG，供 AttendeeTagService 同內容合併邏輯使用。
     * content 以 LOWER() 比對（case-insensitive）；trim 由呼叫端負責。
     * 回傳 List 而非 Optional：production 可能存在 bug 造成的同內容重複 row，
     * 多筆命中時由呼叫端決定合併目標（is_custom=false 優先、其次 id 最小）。
     */
    @Query("""
            SELECT t FROM TagEntity t
            WHERE t.type = :type
              AND LOWER(t.content) = LOWER(:content)
            """)
    List<TagEntity> findByTypeAndContentNormalized(@Param("type") String type,
                                                   @Param("content") String content);
}
