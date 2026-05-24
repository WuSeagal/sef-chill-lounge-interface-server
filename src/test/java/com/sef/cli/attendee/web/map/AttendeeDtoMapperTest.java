package com.sef.cli.attendee.web.map;

import com.sef.cli.api.response.MemberResponse;
import com.sef.cli.api.response.ProfileResponse;
import com.sef.cli.api.response.SocialResponse;
import com.sef.cli.api.response.StickerResponse;
import com.sef.cli.api.response.TagResponse;
import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.entity.AttendeeSocialEntity;
import com.sef.cli.attendee.entity.AttendeeStickerEntity;
import com.sef.cli.tag.entity.TagEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class AttendeeDtoMapperTest {

    private final AttendeeDtoMapper mapper = Mappers.getMapper(AttendeeDtoMapper.class);

    @Test
    void toProfileResponse_copiesAllFields() {
        AttendeeDataEntity entity = AttendeeDataEntity.builder()
                .userId("u-1")
                .username("Foo")
                .furName("FooFur")
                .avatar("/img/a.png")
                .avatarColor("#FFAA00")
                .topicId("t-1")
                .build();

        ProfileResponse r = mapper.toProfileResponse(entity);

        assertThat(r.getUserId()).isEqualTo("u-1");
        assertThat(r.getUsername()).isEqualTo("Foo");
        assertThat(r.getFurName()).isEqualTo("FooFur");
        assertThat(r.getAvatar()).isEqualTo("/img/a.png");
        assertThat(r.getAvatarColor()).isEqualTo("#FFAA00");
        assertThat(r.getTopicId()).isEqualTo("t-1");
    }

    @Test
    void toMemberResponse_dropsTopicId() {
        AttendeeDataEntity entity = AttendeeDataEntity.builder()
                .userId("u-2")
                .username("Bar")
                .furName("BarFur")
                .topicId("t-2")
                .build();

        MemberResponse m = mapper.toMemberResponse(entity);

        assertThat(m.getUserId()).isEqualTo("u-2");
        assertThat(m.getUsername()).isEqualTo("Bar");
        assertThat(m.getFurName()).isEqualTo("BarFur");
    }

    @Test
    void toTagResponse_copies() {
        TagEntity t = TagEntity.builder()
                .tagId("tg-001")
                .type("default")
                .content("宅")
                .build();

        TagResponse r = mapper.toTagResponse(t);

        assertThat(r.getTagId()).isEqualTo("tg-001");
        assertThat(r.getType()).isEqualTo("default");
        assertThat(r.getContent()).isEqualTo("宅");
    }

    @Test
    void toSocialResponse_copies() {
        AttendeeSocialEntity e = AttendeeSocialEntity.builder()
                .id(7L)
                .userId("u-1")
                .platform("twitter")
                .links("https://twitter.com/x")
                .build();

        SocialResponse r = mapper.toSocialResponse(e);

        assertThat(r.getId()).isEqualTo(7L);
        assertThat(r.getPlatform()).isEqualTo("twitter");
        assertThat(r.getLinks()).isEqualTo("https://twitter.com/x");
    }

    @Test
    void toStickerResponse_copies() {
        AttendeeStickerEntity e = AttendeeStickerEntity.builder()
                .id(3L)
                .userId("u-1")
                .stickerNo(1)
                .sticker("/uploads/sticker/a.png")
                .build();

        StickerResponse r = mapper.toStickerResponse(e);

        assertThat(r.getId()).isEqualTo(3L);
        assertThat(r.getStickerNo()).isEqualTo(1);
        assertThat(r.getSticker()).isEqualTo("/uploads/sticker/a.png");
    }
}
