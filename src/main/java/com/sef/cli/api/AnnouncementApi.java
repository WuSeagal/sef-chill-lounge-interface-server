package com.sef.cli.api;

import com.sef.cli.api.request.AnnouncementRequest;
import com.sef.cli.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Announcement API", description = "")
public interface AnnouncementApi {

    @Operation(summary = "設定 / 清除主持人公告（host 限定）",
            description = "host 設定置頂公告；text 空白＝清除。回 200 ApiResponse，並廣播 ANNOUNCEMENT")
    @PostMapping("/announcement")
    ResponseEntity<ApiResponse<Void>> setAnnouncement(@RequestBody AnnouncementRequest req);
}
