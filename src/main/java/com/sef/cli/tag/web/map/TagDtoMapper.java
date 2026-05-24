package com.sef.cli.tag.web.map;

import com.sef.cli.api.response.TagResponse;
import com.sef.cli.tag.entity.TagEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagDtoMapper {
    TagResponse toResponse(TagEntity entity);

    List<TagResponse> toResponseList(List<TagEntity> entities);
}
