package com.sef.cli.attendee.service;

import com.sef.cli.api.request.AddSocialLinkRequest;
import com.sef.cli.attendee.entity.AttendeeSocialEntity;
import com.sef.cli.attendee.repository.AttendeeSocialRepository;
import com.sef.cli.common.exception.ForbiddenException;
import com.sef.cli.common.exception.SocialLinkNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendeeSocialService {

    private final AttendeeSocialRepository attendeeSocialRepository;

    @Transactional
    public AttendeeSocialEntity addSocialLink(String userId, AddSocialLinkRequest req) {
        AttendeeSocialEntity e = AttendeeSocialEntity.builder()
                .userId(userId)
                .platform(req.getPlatform())
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
}
