package com.sef.cli.tag.repository;

import com.sef.cli.config.JpaAuditingConfig;
import com.sef.cli.tag.entity.TagEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Test
    void should_save_default_tag() {
        TagEntity tag = TagEntity.builder()
                .type("species")
                .content("狼")
                .build();

        TagEntity saved = tagRepository.save(tag);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTagId()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
    }

    @Test
    void should_save_custom_tag() {
        TagEntity tag = TagEntity.builder()
                .type("custom")
                .content("使用者自訂文字")
                .build();

        TagEntity saved = tagRepository.save(tag);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTagId()).isNotNull();
    }

    @Test
    void should_auto_generate_tagId_via_prePersist() {
        TagEntity tag = TagEntity.builder()
                .type("hobby")
                .content("畫畫")
                .build();

        TagEntity saved = tagRepository.save(tag);

        assertThat(saved.getTagId()).isNotNull();
        assertThat(saved.getTagId()).isNotEmpty();
    }

    @Test
    void should_find_by_tagId() {
        TagEntity tag = TagEntity.builder()
                .type("species")
                .content("狐狸")
                .build();
        TagEntity saved = tagRepository.save(tag);

        Optional<TagEntity> found = tagRepository.findByTagId(saved.getTagId());

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("狐狸");
    }

    @Test
    void should_find_by_type() {
        long initialSpeciesCount = tagRepository.findByType("test-species").size();

        tagRepository.save(TagEntity.builder().type("test-species").content("狼").build());
        tagRepository.save(TagEntity.builder().type("test-species").content("狐狸").build());
        tagRepository.save(TagEntity.builder().type("test-hobby").content("畫畫").build());

        List<TagEntity> speciesTags = tagRepository.findByType("test-species");

        assertThat(speciesTags).hasSize((int) initialSpeciesCount + 2);
    }

    @Test
    void should_reject_duplicate_tagId() {
        TagEntity first = TagEntity.builder()
                .tagId("fixed-tag-001")
                .type("species")
                .content("狼")
                .build();
        tagRepository.saveAndFlush(first);

        TagEntity duplicate = TagEntity.builder()
                .tagId("fixed-tag-001")
                .type("species")
                .content("狐狸")
                .build();

        assertThatThrownBy(() -> tagRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
