package com.sef.cli.api;

import com.sef.cli.api.response.MemberResponse;
import com.sef.cli.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Tag(name = "Member API", description = "")
public interface MemberApi {

    @Operation(summary = "取得所有 attendees 簡略列表", description = "")
    @GetMapping("/members")
    ResponseEntity<ApiResponse<List<MemberResponse>>> getAll();
}
