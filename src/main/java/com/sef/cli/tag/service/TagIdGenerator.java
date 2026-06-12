package com.sef.cli.tag.service;

import com.sef.cli.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TagIdGenerator {

    private static final char[] BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int SUFFIX_LENGTH = 5;
    private static final Map<String, String> PREFIX_BY_TYPE = Map.of(
            "ROLE", "R",
            "LANGUAGE", "L",
            "FRAMEWORK", "F",
            "DATABASE", "D",
            "DEVOPS", "C",
            "CUSTOM", "CUS"
    );

    private final TagRepository tagRepository;

    public String generate(String type) {
        String normalizedType = normalizeType(type);
        String prefix = PREFIX_BY_TYPE.get(normalizedType);
        if (prefix == null) {
            throw new IllegalArgumentException("unsupported_tag_type");
        }

        return prefix + tagRepository.findTagIdsByTypeAndTagIdStartingWith(normalizedType, prefix).stream()
                .map(existing -> extractNormalizedSuffix(existing, prefix))
                .flatMap(Optional::stream)
                .max(this::compareBase62Value)
                .map(this::incrementBase62)
                .orElse("0".repeat(SUFFIX_LENGTH));
    }

    public String normalizeType(String type) {
        return type == null ? "CUSTOM" : type.trim().toUpperCase();
    }

    private Optional<String> extractNormalizedSuffix(String tagId, String prefix) {
        if (!tagId.startsWith(prefix)) {
            return Optional.empty();
        }
        String suffix = tagId.substring(prefix.length());
        if (suffix.isBlank() || suffix.length() > SUFFIX_LENGTH) {
            return Optional.empty();
        }
        for (char c : suffix.toCharArray()) {
            if (!isBase62Char(c)) {
                return Optional.empty();
            }
        }
        return Optional.of("0".repeat(SUFFIX_LENGTH - suffix.length()) + suffix);
    }

    private String incrementBase62(String value) {
        char[] chars = value.toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            int index = indexOf(chars[i]);
            if (index < BASE62.length - 1) {
                chars[i] = BASE62[index + 1];
                for (int j = i + 1; j < chars.length; j++) {
                    chars[j] = BASE62[0];
                }
                return new String(chars);
            }
        }
        throw new IllegalStateException("tag_id_space_exhausted");
    }

    private int compareBase62Value(String left, String right) {
        for (int i = 0; i < Math.min(left.length(), right.length()); i++) {
            int compared = Integer.compare(indexOf(left.charAt(i)), indexOf(right.charAt(i)));
            if (compared != 0) {
                return compared;
            }
        }
        return Integer.compare(left.length(), right.length());
    }

    private int indexOf(char c) {
        for (int i = 0; i < BASE62.length; i++) {
            if (BASE62[i] == c) {
                return i;
            }
        }
        throw new IllegalArgumentException("invalid_base62_character");
    }

    private boolean isBase62Char(char c) {
        for (char base62Char : BASE62) {
            if (base62Char == c) {
                return true;
            }
        }
        return false;
    }
}
