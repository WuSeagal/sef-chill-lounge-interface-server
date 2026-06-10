package com.sef.cli.attendee.service;

import com.sef.cli.api.request.AddTagRequest;
import com.sef.cli.attendee.entity.AttendeeTagEntity;
import com.sef.cli.attendee.repository.AttendeeTagRepository;
import com.sef.cli.config.JpaAuditingConfig;
import com.sef.cli.tag.config.TagProperties;
import com.sef.cli.tag.entity.TagEntity;
import com.sef.cli.tag.repository.TagRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 整合驗證（task 3.1）：用真實 repository + 真實持久化，驗證 {@code AttendeeTagService.addTag}
 * content 路徑寫進 DB 的實際欄位值——這是本 change 修復的 bug 根因（is_custom 曾落地為 false）。
 * Mockito 單元測試 stub 掉 save() 無法證明欄位真的寫入，故此測試補上 service→DB→讀回 的完整路徑。
 * 繞過 OAuth（auth 與此 bug 正交）；與 data-h2.sql seed 共存：用 unique content prefix。
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class AttendeeTagServiceIntegrationTest {

    @Autowired
    TagRepository tagRepository;

    @Autowired
    AttendeeTagRepository attendeeTagRepository;

    @PersistenceContext
    EntityManager em;

    AttendeeTagService service;

    private static final String UQ = "tdd-int-";

    @BeforeEach
    void setup() {
        service = new AttendeeTagService(tagRepository, attendeeTagRepository, new TagProperties());
    }

    private long junctionCount(String tagId) {
        return attendeeTagRepository.findAll().stream()
                .filter(j -> tagId.equals(j.getTagId())).count();
    }

    @Test
    void createPath_persistsIsCustomTrueAndUppercaseType() {
        TagEntity result = service.addTag("u-int-1",
                new AddTagRequest(null, null, UQ + "滑雪"));
        em.flush();
        em.clear();

        // 從 DB 重新讀回（非快取實例），確認欄位真的落地
        TagEntity persisted = tagRepository.findByTagId(result.getTagId()).orElseThrow();
        assertThat(persisted.isCustom()).isTrue();
        assertThat(persisted.getType()).isEqualTo("CUSTOM");
        assertThat(persisted.getContent()).isEqualTo(UQ + "滑雪");
        assertThat(junctionCount(result.getTagId())).isEqualTo(1);
    }

    @Test
    void mergePath_lowThresholdCustom_reusesRow_holdersAccumulate() {
        // 既有未達標 custom（1 holder）
        TagEntity existing = tagRepository.save(TagEntity.builder()
                .type("CUSTOM").content(UQ + "私房菜").isCustom(true).build());
        attendeeTagRepository.save(AttendeeTagEntity.builder()
                .userId("u-int-a").tagId(existing.getTagId()).build());
        em.flush();
        long tagRowsBefore = tagRepository.count();

        // 另一帳號 POST 同內容
        TagEntity result = service.addTag("u-int-b",
                new AddTagRequest(null, "CUSTOM", UQ + "私房菜"));
        em.flush();

        assertThat(result.getTagId()).isEqualTo(existing.getTagId());
        assertThat(tagRepository.count()).isEqualTo(tagRowsBefore); // 不新建 TAG row
        assertThat(junctionCount(existing.getTagId())).isEqualTo(2); // holders +1
    }

    @Test
    void reusePath_existingDefaultTag_notNewRow_staysNonCustom() {
        TagEntity def = tagRepository.save(TagEntity.builder()
                .type("LANGUAGE").content(UQ + "Rust").isCustom(false).build());
        em.flush();
        long tagRowsBefore = tagRepository.count();

        TagEntity result = service.addTag("u-int-c",
                new AddTagRequest(null, "LANGUAGE", UQ + "Rust"));
        em.flush();
        em.clear();

        assertThat(result.getTagId()).isEqualTo(def.getTagId());
        assertThat(tagRepository.count()).isEqualTo(tagRowsBefore);
        assertThat(tagRepository.findByTagId(def.getTagId()).orElseThrow().isCustom()).isFalse();
    }

    @Test
    void idempotent_whenAlreadyAssociated_noDuplicateJunction() {
        TagEntity def = tagRepository.save(TagEntity.builder()
                .type("LANGUAGE").content(UQ + "Go").isCustom(false).build());
        attendeeTagRepository.save(AttendeeTagEntity.builder()
                .userId("u-int-d").tagId(def.getTagId()).build());
        em.flush();

        // 已持有 → 再 POST 同內容應冪等成功、不重複建 junction
        TagEntity result = service.addTag("u-int-d",
                new AddTagRequest(null, "LANGUAGE", UQ + "Go"));
        em.flush();

        assertThat(result.getTagId()).isEqualTo(def.getTagId());
        assertThat(junctionCount(def.getTagId())).isEqualTo(1);
    }
}
