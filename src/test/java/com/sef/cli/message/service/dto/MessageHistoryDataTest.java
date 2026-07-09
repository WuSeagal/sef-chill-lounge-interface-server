package com.sef.cli.message.service.dto;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.message.entity.MessageEntity;
import com.sef.cli.message.enums.MessageType;
import com.sef.cli.message.service.dto.ReplyPreview;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MessageHistoryDataTest {

    @Test
    void from_includesAvatarColorAndBorderFromAttendee() {
        MessageEntity msg = MessageEntity.builder()
                .messageId("m1").userId("u1").messageType(MessageType.TEXT)
                .content("hi").build();
        AttendeeDataEntity attendee = AttendeeDataEntity.builder()
                .userId("u1").furName("F").avatar("/a.png")
                .avatarColor("#7b9b8f").avatarBorder(true).build();

        MessageHistoryData data = MessageHistoryData.from(msg, attendee, null);

        assertThat(data.avatarColor()).isEqualTo("#7b9b8f");
        assertThat(data.avatarBorder()).isTrue();
    }

    @Test
    void from_nullAttendee_yieldsNullColorAndFalseBorder() {
        MessageEntity msg = MessageEntity.builder()
                .messageId("m2").userId("u2").messageType(MessageType.TEXT).build();

        MessageHistoryData data = MessageHistoryData.from(msg, null, null);

        assertThat(data.avatarColor()).isNull();
        assertThat(data.avatarBorder()).isFalse();
    }

    @Test
    void from_includesReplyPreviewFields() {
        LocalDateTime replyTime = LocalDateTime.of(2026, 5, 20, 14, 2, 15);
        MessageEntity msg = MessageEntity.builder()
                .messageId("m3").userId("u3").messageType(MessageType.TEXT).content("好可愛")
                .replyToMessageId("target").build();
        ReplyPreview preview = new ReplyPreview("u-target", "小白", "看看這張", replyTime);

        MessageHistoryData data = MessageHistoryData.from(msg, null, preview);

        assertThat(data.replyToMessageId()).isEqualTo("target");
        assertThat(data.replyToUserId()).isEqualTo("u-target");
        assertThat(data.replyToFurName()).isEqualTo("小白");
        assertThat(data.replyToContentSnippet()).isEqualTo("看看這張");
        assertThat(data.replyToCreatedDate()).isEqualTo(replyTime);
    }

    @Test
    void from_nullReplyPreviewYieldsNullDerivedReplyFieldsButKeepsReplyToMessageId() {
        MessageEntity msg = MessageEntity.builder()
                .messageId("m4a").userId("u4a").messageType(MessageType.TEXT).content("已刪除的回覆目標")
                .replyToMessageId("target-gone").build();

        MessageHistoryData data = MessageHistoryData.from(msg, null, null);

        assertThat(data.replyToMessageId()).isEqualTo("target-gone");
        assertThat(data.replyToUserId()).isNull();
        assertThat(data.replyToFurName()).isNull();
        assertThat(data.replyToContentSnippet()).isNull();
        assertThat(data.replyToCreatedDate()).isNull();
    }

    @Test
    void from_nonReplyMessageHasNullReplyFields() {
        MessageEntity msg = MessageEntity.builder()
                .messageId("m4").userId("u4").messageType(MessageType.TEXT).content("一般").build();

        MessageHistoryData data = MessageHistoryData.from(msg, null, null);

        assertThat(data.replyToMessageId()).isNull();
        assertThat(data.replyToUserId()).isNull();
        assertThat(data.replyToFurName()).isNull();
        assertThat(data.replyToContentSnippet()).isNull();
        assertThat(data.replyToCreatedDate()).isNull();
    }
}
