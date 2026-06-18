package com.sef.cli.chat.event.response;

/**
 * ANNOUNCEMENT 廣播 payload：主持人公告純文字；text 為 null/空表示清除公告。
 */
public record AnnouncementPayload(String text) {
}
