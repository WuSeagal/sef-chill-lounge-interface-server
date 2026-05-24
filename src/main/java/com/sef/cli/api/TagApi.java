package com.sef.cli.api;

import com.sef.cli.api.response.TagResponse;
import com.sef.cli.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Tag(name = "Tag List API", description = "")
public interface TagApi {

    @Operation(summary = "列出所有非 custom（即現成可選）tag", description = "供 Onboarding / Dashboard 編輯時顯示可選清單")
    @GetMapping("/tags")
    ResponseEntity<ApiResponse<List<TagResponse>>> getSelectableTags();
}
