package com.sef.cli.api;

import com.sef.cli.api.request.BanRequest;
import com.sef.cli.api.response.BlacklistEntryResponse;
import com.sef.cli.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Blacklist API", description = "host 維護黑名單（列出 / 封禁 / 解封）")
public interface BlacklistApi {

    @Operation(summary = "取得目前黑名單（host 限定）",
            description = "回傳 banned 使用者清單（userId / furName / username）")
    @GetMapping("/blacklist")
    ResponseEntity<ApiResponse<List<BlacklistEntryResponse>>> getBlacklist();

    @Operation(summary = "封禁使用者（host 限定）",
            description = "body { userId }；userId 不存在回失敗 ApiResponse 且不改資料")
    @PostMapping("/blacklist")
    ResponseEntity<ApiResponse<Void>> ban(@RequestBody BanRequest req);

    @Operation(summary = "解封使用者（host 限定）",
            description = "body { userId }；設 banned=false。回 ApiResponse 信封（不回 204）")
    @PostMapping("/blacklist/remove")
    ResponseEntity<ApiResponse<Void>> unban(@RequestBody BanRequest req);
}
