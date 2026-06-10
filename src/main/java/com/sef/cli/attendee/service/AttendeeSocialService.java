package com.sef.cli.attendee.service;

import com.sef.cli.api.request.AddSocialLinkRequest;
import com.sef.cli.attendee.entity.AttendeeSocialEntity;
import com.sef.cli.attendee.enums.PlatformEnum;
import com.sef.cli.attendee.repository.AttendeeSocialRepository;
import com.sef.cli.common.exception.ForbiddenException;
import com.sef.cli.common.exception.InvalidPlatformException;
import com.sef.cli.common.exception.InvalidSocialUrlException;
import com.sef.cli.common.exception.SocialLinkNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendeeSocialService {

    private final AttendeeSocialRepository attendeeSocialRepository;
    private final SocialUrlValidator socialUrlValidator;

    @Transactional
    public AttendeeSocialEntity addSocialLink(String userId, AddSocialLinkRequest req) {
        PlatformEnum platform;
        try {
            platform = parsePlatform(req.getPlatform());
        } catch (InvalidPlatformException ex) {
            log.warn("[SOCIAL_ADD_FAIL] 平台不合法, userId={}, platform={}", userId, req.getPlatform());
            throw ex;
        }
        SocialUrlValidator.Result result = socialUrlValidator.validate(platform, req.getLinks());
        if (result != SocialUrlValidator.Result.OK) {
            log.warn("[SOCIAL_ADD_FAIL] URL 驗證失敗, userId={}, platform={}, reason={}", userId, platform, result);
            throw new InvalidSocialUrlException(result);
        }
        AttendeeSocialEntity e = AttendeeSocialEntity.builder()
                .userId(userId)
                .platform(platform)
                .links(req.getLinks())
                .build();
        AttendeeSocialEntity saved = attendeeSocialRepository.save(e);
        log.info("[SOCIAL_ADD] social link 新增成功, userId={}, platform={}", userId, platform);
        return saved;
    }

    @Transactional
    public void removeSocialLink(String userId, Long id) {
        AttendeeSocialEntity e = attendeeSocialRepository.findById(id)
                .orElseThrow(SocialLinkNotFoundException::new);
        if (!e.getUserId().equals(userId)) {
            throw new ForbiddenException();
        }
        attendeeSocialRepository.deleteById(id);
        log.info("[SOCIAL_REMOVE] social link 移除成功, userId={}, id={}", userId, id);
    }

    private PlatformEnum parsePlatform(String raw) {
        try {
            return PlatformEnum.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidPlatformException(raw);
        }
    }
}
