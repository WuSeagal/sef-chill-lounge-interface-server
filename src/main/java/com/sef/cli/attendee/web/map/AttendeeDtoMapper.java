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
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttendeeDtoMapper {

    ProfileResponse toProfileResponse(AttendeeDataEntity entity);

    // tags 由 MemberController 以批次查詢後另行填入（entity 無 tags 來源），此處 ignore 避免未對應警告。
    @Mapping(target = "tags", ignore = true)
    MemberResponse toMemberResponse(AttendeeDataEntity entity);

    List<MemberResponse> toMemberResponseList(List<AttendeeDataEntity> entities);

    TagResponse toTagResponse(TagEntity tag);

    @Mapping(target = "platform", expression = "java(entity.getPlatform().name())")
    SocialResponse toSocialResponse(AttendeeSocialEntity entity);

    StickerResponse toStickerResponse(AttendeeStickerEntity entity);
}
