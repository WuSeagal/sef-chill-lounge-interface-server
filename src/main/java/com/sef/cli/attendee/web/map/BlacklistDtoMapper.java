package com.sef.cli.attendee.web.map;

import com.sef.cli.api.response.BlacklistEntryResponse;
import com.sef.cli.attendee.entity.AttendeeDataEntity;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 黑名單顯示用 mapper：AttendeeDataEntity → BlacklistEntryResponse（userId/furName/username 同名直接對應）。
 */
@Mapper(componentModel = "spring")
public interface BlacklistDtoMapper {

    BlacklistEntryResponse toEntry(AttendeeDataEntity entity);

    List<BlacklistEntryResponse> toEntryList(List<AttendeeDataEntity> entities);
}
