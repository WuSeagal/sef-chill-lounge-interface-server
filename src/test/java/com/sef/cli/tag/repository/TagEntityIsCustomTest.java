package com.sef.cli.tag.repository;

import com.sef.cli.tag.entity.TagEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TagEntityIsCustomTest {

    @Autowired
    TagRepository tagRepository;

    @Test
    void persistsIsCustomFlag() {
        TagEntity custom = TagEntity.builder()
                .type("CUSTOM").content("露營").isCustom(true).build();
        tagRepository.save(custom);

        TagEntity loaded = tagRepository.findByTagId(custom.getTagId()).orElseThrow();
        assertThat(loaded.isCustom()).isTrue();

        TagEntity def = TagEntity.builder()
                .type("LANGUAGE").content("Java").isCustom(false).build();
        tagRepository.save(def);

        TagEntity loadedDef = tagRepository.findByTagId(def.getTagId()).orElseThrow();
        assertThat(loadedDef.isCustom()).isFalse();
    }
}
