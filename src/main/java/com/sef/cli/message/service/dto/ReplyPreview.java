package com.sef.cli.message.service.dto;

import java.time.LocalDateTime;

/**
 * 回覆預覽的即時解析結果（不落庫，讀取/廣播時由 {@code MessageService} 查詢衍生）。
 * {@code targetUserId} 供前端比對 {@code PROFILE_UPDATED} 事件做即時 patch。
 */
public record ReplyPreview(
        String targetUserId,
        String furName,
        String contentSnippet,
        LocalDateTime createdDate
) {
}
