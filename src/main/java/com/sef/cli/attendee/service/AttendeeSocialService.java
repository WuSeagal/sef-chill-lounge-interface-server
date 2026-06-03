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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendeeSocialService {

    private final AttendeeSocialRepository attendeeSocialRepository;
    private final SocialUrlValidator socialUrlValidator;

    @Transactional
    public AttendeeSocialEntity addSocialLink(String userId, AddSocialLinkRequest req) {
        PlatformEnum platform = parsePlatform(req.getPlatform());
        SocialUrlValidator.Result result = socialUrlValidator.validate(platform, req.getLinks());
        if (result != SocialUrlValidator.Result.OK) {
            throw new InvalidSocialUrlException(result);
        }
        AttendeeSocialEntity e = AttendeeSocialEntity.builder()
                .userId(userId)
                .platform(platform)
                .links(req.getLinks())
                .build();
        return attendeeSocialRepository.save(e);
    }

    @Transactional
    public void removeSocialLink(String userId, Long id) {
        AttendeeSocialEntity e = attendeeSocialRepository.findById(id)
                .orElseThrow(SocialLinkNotFoundException::new);
        if (!e.getUserId().equals(userId)) {
            throw new ForbiddenException();
        }
        attendeeSocialRepository.deleteById(id);
    }

    private PlatformEnum parsePlatform(String raw) {
        try {
            return PlatformEnum.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidPlatformException(raw);
        }
    }
}
