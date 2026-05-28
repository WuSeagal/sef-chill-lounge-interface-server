package com.sef.cli.api;

import com.sef.cli.api.response.TagResponse;
import com.sef.cli.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Tag(name = "Tag List API", description = "")
public interface TagApi {

    @Operation(summary = "依 type 分組列出可選 TAG", description = "排除 isCustom=true 且 holders 數量未達 threshold 的 tag。回傳 6 個 type key 的 Map(空陣列補齊)。")
    @GetMapping("/tags")
    ResponseEntity<ApiResponse<Map<String, List<TagResponse>>>> getSelectableTags();
}
