package com.sef.cli.topic.web;

import com.sef.cli.api.TopicApi;
import com.sef.cli.api.response.TopicResponse;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.topic.service.TopicService;
import com.sef.cli.topic.web.map.TopicDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TopicController implements TopicApi {

    private final TopicService topicService;
    private final TopicDtoMapper topicDtoMapper;

    @Override
    public ResponseEntity<ApiResponse<TopicResponse>> getRandom() {
        TopicResponse data = topicDtoMapper.toResponse(topicService.pickRandom());
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
