package com.sef.cli.topic.web.map;

import com.sef.cli.api.response.TopicResponse;
import com.sef.cli.topic.entity.TopicEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TopicDtoMapper {
    TopicResponse toResponse(TopicEntity entity);
}
