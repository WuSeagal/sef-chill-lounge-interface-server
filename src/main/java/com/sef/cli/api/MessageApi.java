package com.sef.cli.api;

import com.sef.cli.api.request.RemoveMessageRequest;
import com.sef.cli.api.response.MessageResponse;
import com.sef.cli.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Message API", description = "")
public interface MessageApi {

    @Operation(summary = "取得聊天室歷史訊息", description = "單一聊天室的 cursor-based 歷史查詢")
    @GetMapping("/messages")
    ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "50") int limit
    );

    @Operation(summary = "刪除聊天訊息（host 限定）", description = "host 軟刪除訊息，成功後廣播 MESSAGE_DELETED，回 200 ApiResponse")
    @PostMapping("/messages/remove")
    ResponseEntity<ApiResponse<Void>> removeMessage(@RequestBody RemoveMessageRequest req);
}
