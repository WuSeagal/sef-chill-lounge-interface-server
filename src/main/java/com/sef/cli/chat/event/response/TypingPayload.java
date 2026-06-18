package com.sef.cli.chat.event.response;

/**
 * TYPING 廣播 payload：標示某使用者正在輸入。自帶顯示欄位（furName / avatar / avatarColor）
 * 供前端直接呈現頭像，無需反查 members 名單。查無 attendee 時顯示欄位為 null。
 */
public record TypingPayload(String userId, String furName, String avatar, String avatarColor) {
}
