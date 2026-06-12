package com.sef.cli.tag.repository;

import com.sef.cli.config.JpaAuditingConfig;
import com.sef.cli.tag.entity.TagEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 驗證 {@code findByTypeAndContentNormalized}（供 AttendeeTagService 同內容合併邏輯使用）。
 * content 比對為 case-insensitive（LOWER），trim 由呼叫端負責。
 * 與 data-h2.sql seed 共存：測試資料用 unique content prefix 避免碰撞。
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class TagRepositoryFindByTypeAndContentTest {

    @Autowired
    TagRepository tagRepository;

    private static final String UQ = "tdd-fbtc-"; // unique prefix，避免與 seed 碰撞

    @Test
    void matches_default_tag() {
        TagEntity def = tagRepository.save(TagEntity.builder()
                .tagId("L92001")
                .type("LANGUAGE").content(UQ + "Kotlin").isCustom(false).build());

        List<TagEntity> found = tagRepository.findByTypeAndContentNormalized("LANGUAGE", UQ + "Kotlin");

        assertThat(found).extracting(TagEntity::getTagId).containsExactly(def.getTagId());
    }

    @Test
    void matches_lowThreshold_custom_tag() {
        TagEntity custom = tagRepository.save(TagEntity.builder()
                .tagId("CUS92001")
                .type("CUSTOM").content(UQ + "私房菜").isCustom(true).build());

        List<TagEntity> found = tagRepository.findByTypeAndContentNormalized("CUSTOM", UQ + "私房菜");

        assertThat(found).extracting(TagEntity::getTagId).containsExactly(custom.getTagId());
    }

    @Test
    void matches_case_insensitive() {
        TagEntity tag = tagRepository.save(TagEntity.builder()
                .tagId("L92002")
                .type("LANGUAGE").content(UQ + "Java").isCustom(false).build());

        List<TagEntity> found = tagRepository.findByTypeAndContentNormalized("LANGUAGE", UQ + "java");

        assertThat(found).extracting(TagEntity::getTagId).containsExactly(tag.getTagId());
    }

    @Test
    void does_not_match_different_type_same_content() {
        tagRepository.save(TagEntity.builder()
                .tagId("L92003")
                .type("LANGUAGE").content(UQ + "Go").isCustom(false).build());

        List<TagEntity> found = tagRepository.findByTypeAndContentNormalized("CUSTOM", UQ + "Go");

        assertThat(found).isEmpty();
    }

    @Test
    void returns_empty_when_no_match() {
        List<TagEntity> found = tagRepository.findByTypeAndContentNormalized("CUSTOM", UQ + "不存在的內容");

        assertThat(found).isEmpty();
    }

    @Test
    void returns_all_rows_when_duplicate_content() {
        TagEntity a = tagRepository.save(TagEntity.builder()
                .tagId("CUS92002")
                .type("CUSTOM").content(UQ + "重複").isCustom(true).build());
        TagEntity b = tagRepository.save(TagEntity.builder()
                .tagId("CUS92003")
                .type("CUSTOM").content(UQ + "重複").isCustom(true).build());

        List<TagEntity> found = tagRepository.findByTypeAndContentNormalized("CUSTOM", UQ + "重複");

        assertThat(found).extracting(TagEntity::getTagId)
                .containsExactlyInAnyOrder(a.getTagId(), b.getTagId());
    }
}
