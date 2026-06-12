package com.sef.cli.tag.repository;

import com.sef.cli.config.JpaAuditingConfig;
import com.sef.cli.tag.entity.TagEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
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
                .tagId("R99999")
                .type("ROLE")
                .content("狼")
                .isCustom(false)
                .build();

        TagEntity saved = tagRepository.save(tag);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTagId()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
    }

    @Test
    void should_save_custom_tag() {
        TagEntity tag = TagEntity.builder()
                .tagId("CUS99999")
                .type("CUSTOM")
                .content("使用者自訂文字")
                .isCustom(true)
                .build();

        TagEntity saved = tagRepository.save(tag);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTagId()).isNotNull();
    }

    @Test
    void should_require_tagId_beforePersist() {
        TagEntity tag = TagEntity.builder()
                .type("CUSTOM")
                .content("畫畫")
                .isCustom(true)
                .build();

        assertThatThrownBy(() -> tagRepository.saveAndFlush(tag))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tag_id_required");
    }

    @Test
    void should_find_by_tagId() {
        TagEntity tag = TagEntity.builder()
                .tagId("R99998")
                .type("ROLE")
                .content("狐狸")
                .isCustom(false)
                .build();
        TagEntity saved = tagRepository.save(tag);

        Optional<TagEntity> found = tagRepository.findByTagId(saved.getTagId());

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("狐狸");
    }

    @Test
    void should_find_by_type() {
        long initialSpeciesCount = tagRepository.findByType("ROLE_TEST").size();

        tagRepository.save(TagEntity.builder().tagId("RT0001").type("ROLE_TEST").content("狼").isCustom(false).build());
        tagRepository.save(TagEntity.builder().tagId("RT0002").type("ROLE_TEST").content("狐狸").isCustom(false).build());
        tagRepository.save(TagEntity.builder().tagId("HT0001").type("HOBBY_TEST").content("畫畫").isCustom(true).build());

        List<TagEntity> speciesTags = tagRepository.findByType("ROLE_TEST");

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

    @Test
    void should_list_tagIds_by_type_and_prefix() {
        tagRepository.saveAndFlush(TagEntity.builder().tagId("LX000D").type("LANGUAGE").content("Java").isCustom(false).build());
        tagRepository.saveAndFlush(TagEntity.builder().tagId("LX000F").type("LANGUAGE").content("TypeScript").isCustom(false).build());
        tagRepository.saveAndFlush(TagEntity.builder().tagId("LX9999Z").type("ROLE").content("NotLanguage").isCustom(false).build());

        List<String> found = tagRepository.findTagIdsByTypeAndTagIdStartingWith("LANGUAGE", "LX");

        assertThat(found).containsExactlyInAnyOrder("LX000D", "LX000F");
    }

    @Test
    void seedTagIdsShouldUsePrefixedFiveCharSuffixes() {
        List<String> seedLikeIds = tagRepository.findAll().stream()
                .map(TagEntity::getTagId)
                .filter(id -> id.matches("^(R|L|F|D|C)[0-9a-zA-Z]{5}$") || id.matches("^CUS[0-9a-zA-Z]{5}$"))
                .toList();

        assertThat(seedLikeIds).contains("L00001", "R00001", "F00001", "D00001", "C00001", "CUS00001");
    }
}
