package com.sef.cli.tag.service;

import com.sef.cli.tag.entity.TagEntity;
import com.sef.cli.tag.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagIdGeneratorTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagIdGenerator generator;

    @Test
    void shouldGenerateLanguageIdFromExistingMax() {
        when(tagRepository.findTagIdsByTypeAndTagIdStartingWith("LANGUAGE", "L"))
                .thenReturn(List.of("L0000D"));

        String next = generator.generate("LANGUAGE");

        assertThat(next).isEqualTo("L0000E");
    }

    @Test
    void shouldGenerateFirstCustomIdWhenNoExistingRows() {
        when(tagRepository.findTagIdsByTypeAndTagIdStartingWith("CUSTOM", "CUS"))
                .thenReturn(List.of());

        String next = generator.generate("CUSTOM");

        assertThat(next).isEqualTo("CUS00000");
    }

    @Test
    void shouldPreferBase62MaxNotLexicographicMax() {
        when(tagRepository.findTagIdsByTypeAndTagIdStartingWith("CUSTOM", "CUS"))
                .thenReturn(List.of("CUS0000z", "CUS0000A"));

        String next = generator.generate("CUSTOM");

        assertThat(next).isEqualTo("CUS0000B");
    }

    @Test
    void shouldNormalizeLegacyShortSuffixBeforeIncrementing() {
        when(tagRepository.findTagIdsByTypeAndTagIdStartingWith("CUSTOM", "CUS"))
                .thenReturn(List.of("CUS001", "CUS002"));

        String next = generator.generate("CUSTOM");

        assertThat(next).isEqualTo("CUS00003");
    }

    @Test
    void shouldIgnoreMalformedLegacyIdsThatMatchPrefix() {
        when(tagRepository.findTagIdsByTypeAndTagIdStartingWith("DATABASE", "D"))
                .thenReturn(List.of("D00004", "Dlegacy77", "D1234567"));

        String next = generator.generate("DATABASE");

        assertThat(next).isEqualTo("D00005");
    }
}
