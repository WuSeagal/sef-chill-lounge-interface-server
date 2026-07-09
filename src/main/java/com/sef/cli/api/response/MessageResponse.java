package com.sef.cli.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long cursorId;
    private String messageId;
    private String userId;
    private String messageType;
    private String furName;
    private String avatar;
    private String avatarColor;
    private boolean avatarBorder;
    private String content;
    private List<String> imageUrls;
    private String stickerImageUrl;
    private LocalDateTime createdDate;
    private String replyToMessageId;
    private String replyToUserId;
    private String replyToFurName;
    private String replyToContentSnippet;
    private LocalDateTime replyToCreatedDate;
}
