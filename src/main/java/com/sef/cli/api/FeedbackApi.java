package com.sef.cli.api;

import com.sef.cli.api.request.FeedbackRequest;
import com.sef.cli.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "意見回饋 API", description = "")
public interface FeedbackApi {

    @Operation(summary = "送出意見回饋", description = "寄送意見回饋信件到維運信箱")
    @PostMapping("/feedback")
    ResponseEntity<ApiResponse<Void>> submit(@Valid @RequestBody FeedbackRequest request);
}
