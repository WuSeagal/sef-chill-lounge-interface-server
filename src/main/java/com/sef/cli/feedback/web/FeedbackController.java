package com.sef.cli.feedback.web;

import com.sef.cli.api.FeedbackApi;
import com.sef.cli.api.request.FeedbackRequest;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.feedback.service.FeedbackMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FeedbackController implements FeedbackApi {

    private final FeedbackMailService feedbackMailService;

    @Override
    public ResponseEntity<ApiResponse<Void>> submit(FeedbackRequest request) {
        feedbackMailService.send(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
