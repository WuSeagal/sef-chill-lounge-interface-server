package com.sef.cli.message.service.dto;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.message.entity.MessageEntity;
import com.sef.cli.message.enums.MessageType;
import org.junit.jupiter.api.Test;

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

        MessageHistoryData data = MessageHistoryData.from(msg, attendee);

        assertThat(data.avatarColor()).isEqualTo("#7b9b8f");
        assertThat(data.avatarBorder()).isTrue();
    }

    @Test
    void from_nullAttendee_yieldsNullColorAndFalseBorder() {
        MessageEntity msg = MessageEntity.builder()
                .messageId("m2").userId("u2").messageType(MessageType.TEXT).build();

        MessageHistoryData data = MessageHistoryData.from(msg, null);

        assertThat(data.avatarColor()).isNull();
        assertThat(data.avatarBorder()).isFalse();
    }
}
