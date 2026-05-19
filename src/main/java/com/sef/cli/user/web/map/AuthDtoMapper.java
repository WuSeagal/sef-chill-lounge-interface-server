package com.sef.cli.user.web.map;

import com.sef.cli.api.response.AuthResponse;
import com.sef.cli.user.entity.AdminUserEntity;
import org.mapstruct.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.mapstruct.CollectionMappingStrategy.TARGET_IMMUTABLE;
import static org.mapstruct.NullValueMappingStrategy.RETURN_DEFAULT;

@Mapper(componentModel = "spring", collectionMappingStrategy = TARGET_IMMUTABLE, nullValueMappingStrategy = RETURN_DEFAULT)
public interface AuthDtoMapper {
    Logger log = LoggerFactory.getLogger(AuthDtoMapper.class.getName());

    AuthResponse toResponse(AdminUserEntity entity);
    List<AuthResponse> toResponse(List<AdminUserEntity> entity);
}
