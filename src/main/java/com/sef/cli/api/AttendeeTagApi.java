package com.sef.cli.api;

import com.sef.cli.api.request.AddTagRequest;
import com.sef.cli.api.request.RemoveTagRequest;
import com.sef.cli.api.response.TagResponse;
import com.sef.cli.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Attendee Tag API", description = "")
public interface AttendeeTagApi {

    @Operation(summary = "新增 tag 關聯（既有 / custom）", description = "")
    @PostMapping("/user/tags")
    ResponseEntity<ApiResponse<TagResponse>> addTag(@RequestBody AddTagRequest req);

    @Operation(summary = "刪除自己的 tag 關聯", description = "")
    @PostMapping("/user/tags/remove")
    ResponseEntity<ApiResponse<Object>> removeTag(@RequestBody RemoveTagRequest req);
}
