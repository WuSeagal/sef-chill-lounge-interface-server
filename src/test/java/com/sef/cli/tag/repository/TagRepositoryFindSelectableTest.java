package com.sef.cli.tag.repository;

import com.sef.cli.attendee.entity.AttendeeTagEntity;
import com.sef.cli.attendee.repository.AttendeeTagRepository;
import com.sef.cli.config.JpaAuditingConfig;
import com.sef.cli.tag.entity.TagEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class TagRepositoryFindSelectableTest {

    @Autowired
    TagRepository tagRepository;

    @Autowired
    AttendeeTagRepository attendeeTagRepository;

    @Test
    void includesAllDefaultAndCustomWithAtLeastThreshold() {
        tagRepository.deleteAll();
        attendeeTagRepository.deleteAll();

        TagEntity defaultTag = tagRepository.save(TagEntity.builder()
                .type("LANGUAGE").content("Java").isCustom(false).build());
        TagEntity lowCustom = tagRepository.save(TagEntity.builder()
                .type("CUSTOM").content("少人有").isCustom(true).build());
        TagEntity popularCustom = tagRepository.save(TagEntity.builder()
                .type("CUSTOM").content("熱門").isCustom(true).build());

        for (int i = 1; i <= 5; i++) {
            attendeeTagRepository.save(AttendeeTagEntity.builder()
                    .userId("u-" + i).tagId(popularCustom.getTagId()).build());
        }
        attendeeTagRepository.save(AttendeeTagEntity.builder()
                .userId("u-x").tagId(lowCustom.getTagId()).build());

        List<TagEntity> selectable = tagRepository.findSelectableTags(5);

        assertThat(selectable).extracting(TagEntity::getTagId)
                .containsExactlyInAnyOrder(defaultTag.getTagId(), popularCustom.getTagId())
                .doesNotContain(lowCustom.getTagId());
    }
}
