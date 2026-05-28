package com.sef.cli.tag.web;

import com.sef.cli.api.TagApi;
import com.sef.cli.api.response.TagResponse;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.tag.config.TagProperties;
import com.sef.cli.tag.entity.TagEntity;
import com.sef.cli.tag.repository.TagRepository;
import com.sef.cli.tag.web.map.TagDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class TagController implements TagApi {

    private static final List<String> TYPE_ORDER =
            List.of("ROLE", "LANGUAGE", "FRAMEWORK", "DATABASE", "DEVOPS", "CUSTOM");

    private final TagRepository tagRepository;
    private final TagDtoMapper tagDtoMapper;
    private final TagProperties tagProperties;

    @Override
    public ResponseEntity<ApiResponse<Map<String, List<TagResponse>>>> getSelectableTags() {
        List<TagEntity> selectable = tagRepository.findSelectableTags(
                tagProperties.getCustomHoldersThreshold());

        Map<String, List<TagResponse>> byType = selectable.stream()
                .map(tagDtoMapper::toResponse)
                .collect(Collectors.groupingBy(TagResponse::getType));

        Map<String, List<TagResponse>> grouped = new LinkedHashMap<>();
        for (String type : TYPE_ORDER) {
            grouped.put(type, byType.getOrDefault(type, List.of()));
        }
        return ResponseEntity.ok(ApiResponse.success(grouped));
    }
}
