package com.sef.cli.message.entity;

import com.sef.cli.message.enums.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Table(name = "MESSAGES")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, length = 64)
    private String messageId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 32)
    private MessageType messageType;

    @Column(name = "content", length = 500)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls")
    private List<String> imageUrls;

    @Column(name = "sticker_image_url", length = 1024)
    private String stickerImageUrl;

    // 唯一持久化的回覆關聯欄位（FK-like，值永不變）。作者 furName / 內容摘要 / 建立時間
    // 皆不落庫，一律於讀取歷史或組裝廣播時即時查詢衍生（見 MessageService#resolveReplyPreview），
    // 以保證改名/刪除後每次讀取都反映當前狀態（快照會有「離線改名後重新載入仍顯示舊名字」的洞）。
    @Column(name = "reply_to_message_id", length = 64)
    private String replyToMessageId;

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @PrePersist
    void assignMessageId() {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalStateException("message_id_required");
        }
    }
}
