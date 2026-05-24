package com.sef.cli.api;

import com.sef.cli.api.response.TopicResponse;
import com.sef.cli.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "Topic API", description = "")
public interface TopicApi {

    @Operation(summary = "抽一張隨機 topic（不寫 DB）", description = "供 Onboarding 進場顯示用")
    @GetMapping("/topics/random")
    ResponseEntity<ApiResponse<TopicResponse>> getRandom();
}
