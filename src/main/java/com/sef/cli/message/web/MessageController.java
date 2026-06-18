package com.sef.cli.message.web;

import com.sef.cli.api.MessageApi;
import com.sef.cli.api.request.RemoveMessageRequest;
import com.sef.cli.api.response.MessageResponse;
import com.sef.cli.chat.event.ChatEnvelope;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.event.response.MessageDeletedPayload;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.message.service.MessageService;
import com.sef.cli.message.web.map.MessageDtoMapper;
import com.sef.cli.user.entity.AdminUserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MessageController implements MessageApi {

    private final MessageService messageService;
    private final MessageDtoMapper messageDtoMapper;
    private final ChatBroadcastService chatBroadcastService;

    @Override
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(LocalDateTime before, Long beforeId, int limit) {
        List<MessageResponse> data = messageService.loadHistory(before, beforeId, limit).stream()
                .map(messageDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> removeMessage(RemoveMessageRequest req) {
        String messageId = req.getMessageId();
        String requesterId = currentUserId();
        boolean changed = messageService.softDelete(messageId, requesterId);
        if (changed) {
            // 單一移除路徑：所有 client（含發起刪除的 host 自己）皆靠此廣播移除。
            chatBroadcastService.broadcastToAll(new ChatEnvelope<>(
                    ChatEventType.MESSAGE_DELETED,
                    System.currentTimeMillis(),
                    new MessageDeletedPayload(messageId)
            ));
        }
        log.info("[MESSAGE_DELETE] host={} messageId={} changed={}", requesterId, messageId, changed);
        // 回統一 ApiResponse 信封（code 200）：前端 axios 攔截器要求所有回應皆為 ApiResponse，
        // 否則 204 無 body 會被當成失敗（res.code !== 200 → reject）。
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AdminUserEntity user)) {
            throw new InsufficientAuthenticationException("no user");
        }
        return user.getProviderUserId();
    }
}
