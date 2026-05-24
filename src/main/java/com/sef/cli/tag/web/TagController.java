package com.sef.cli.tag.web;

import com.sef.cli.api.TagApi;
import com.sef.cli.api.response.TagResponse;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.tag.repository.TagRepository;
import com.sef.cli.tag.web.map.TagDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TagController implements TagApi {

    private final TagRepository tagRepository;
    private final TagDtoMapper tagDtoMapper;

    @Override
    public ResponseEntity<ApiResponse<List<TagResponse>>> getSelectableTags() {
        // sef 既有 schema 中 tag.type 為 species / hobby / custom 等；
        // onboarding 用 GET /tags 列「可選清單」，排除 custom 即可，其餘皆視為現成可選。
        List<TagResponse> data = tagDtoMapper.toResponseList(tagRepository.findByTypeNot("custom"));
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
