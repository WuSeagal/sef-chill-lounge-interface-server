package com.sef.cli.message.web.map;

import com.sef.cli.api.response.MessageResponse;
import com.sef.cli.message.service.dto.MessageHistoryData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageDtoMapper {

    @Mapping(target = "messageType", expression = "java(data.messageType().name())")
    MessageResponse toResponse(MessageHistoryData data);
}
