package com.sef.cli.chat.event.response;

/**
 * MESSAGE_DELETED 廣播 payload：通知 /chat 與 /dashboard 移除指定訊息。
 */
public record MessageDeletedPayload(String messageId) {
}
