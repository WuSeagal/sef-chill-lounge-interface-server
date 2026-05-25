package com.sef.cli.message.web;

import com.sef.cli.api.MessageApi;
import com.sef.cli.api.response.MessageResponse;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.message.service.MessageService;
import com.sef.cli.message.web.map.MessageDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MessageController implements MessageApi {

    private final MessageService messageService;
    private final MessageDtoMapper messageDtoMapper;

    @Override
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(LocalDateTime before, Long beforeId, int limit) {
        List<MessageResponse> data = messageService.loadHistory(before, beforeId, limit).stream()
                .map(messageDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
