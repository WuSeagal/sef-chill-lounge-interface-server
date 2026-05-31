package com.sef.cli.attendee.service;

import com.sef.cli.api.request.CreateProfileRequest;
import com.sef.cli.api.request.UpdateProfileRequest;
import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.entity.AttendeeSocialEntity;
import com.sef.cli.attendee.entity.AttendeeStickerEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.attendee.repository.AttendeeSocialRepository;
import com.sef.cli.attendee.repository.AttendeeStickerRepository;
import com.sef.cli.attendee.repository.AttendeeTagRepository;
import com.sef.cli.common.exception.ProfileAlreadyExistsException;
import com.sef.cli.common.exception.ProfileNotFoundException;
import com.sef.cli.tag.entity.TagEntity;
import com.sef.cli.topic.entity.TopicEntity;
import com.sef.cli.topic.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendeeService {

    public static final String DEFAULT_AVATAR_COLOR = "#ffffff";

    private final AttendeeDataRepository attendeeDataRepository;
    private final AttendeeTagRepository attendeeTagRepository;
    private final AttendeeSocialRepository attendeeSocialRepository;
    private final AttendeeStickerRepository attendeeStickerRepository;
    private final TopicService topicService;

    public AttendeeDataEntity getProfileOrThrow(String userId) {
        return attendeeDataRepository.findByUserId(userId)
                .orElseThrow(ProfileNotFoundException::new);
    }

    public Optional<AttendeeDataEntity> findProfile(String userId) {
        return attendeeDataRepository.findByUserId(userId);
    }

    @Transactional
    public AttendeeDataEntity createProfile(String userId, CreateProfileRequest req) {
        if (attendeeDataRepository.existsByUserId(userId)) {
            throw new ProfileAlreadyExistsException();
        }
        String resolvedUsername = "user-" + userId;

        String topicId = null;
        if (req.getTopicId() != null && !req.getTopicId().isBlank()) {
            topicService.findByTopicIdOrThrow(req.getTopicId()); // 驗證存在
            topicId = req.getTopicId();
        }

        AttendeeDataEntity entity = AttendeeDataEntity.builder()
                .userId(userId)
                .username(resolvedUsername)
                .furName(req.getFurName())
                .avatar(req.getAvatar())
                .avatarColor(req.getAvatarColor() != null && !req.getAvatarColor().isBlank()
                        ? req.getAvatarColor() : DEFAULT_AVATAR_COLOR)
                .avatarBorder(req.getAvatarBorder() != null ? req.getAvatarBorder() : false)
                .topicId(topicId)
                .build();
        return attendeeDataRepository.save(entity);
    }

    @Transactional
    public AttendeeDataEntity updateProfile(String userId, UpdateProfileRequest req) {
        AttendeeDataEntity entity = getProfileOrThrow(userId);
        if (req.getFurName() != null) entity.setFurName(req.getFurName());
        if (req.getAvatar() != null) entity.setAvatar(req.getAvatar());
        if (req.getAvatarColor() != null) entity.setAvatarColor(req.getAvatarColor());
        if (req.getAvatarBorder() != null) entity.setAvatarBorder(req.getAvatarBorder());
        if (req.getTopicId() != null) {
            topicService.findByTopicIdOrThrow(req.getTopicId());
            entity.setTopicId(req.getTopicId());
        }
        return attendeeDataRepository.save(entity);
    }

    @Transactional
    public TopicEntity redrawTopicCard(String userId) {
        AttendeeDataEntity entity = getProfileOrThrow(userId);
        TopicEntity newTopic = (entity.getTopicId() == null)
                ? topicService.pickRandom()
                : topicService.pickRandomExcluding(entity.getTopicId());
        entity.setTopicId(newTopic.getTopicId());
        attendeeDataRepository.save(entity);
        return newTopic;
    }

    /** Service 層 record，用於 nested response 組裝。Controller 負責轉 ProfileDetailResponse。 */
    public record ProfileDetail(
            AttendeeDataEntity entity,
            List<TagEntity> tags,
            List<AttendeeSocialEntity> socials,
            List<AttendeeStickerEntity> stickers,
            TopicEntity topic
    ) {
    }

    public ProfileDetail loadDetail(String userId) {
        AttendeeDataEntity entity = getProfileOrThrow(userId);
        List<TagEntity> tags = attendeeTagRepository.findTagsByUserId(userId);
        List<AttendeeSocialEntity> socials = attendeeSocialRepository.findByUserId(userId);
        List<AttendeeStickerEntity> stickers = attendeeStickerRepository.findByUserIdOrderByStickerNo(userId);
        TopicEntity topic = (entity.getTopicId() == null) ? null
                : topicService.findByTopicIdOrThrow(entity.getTopicId());
        return new ProfileDetail(entity, tags, socials, stickers, topic);
    }
}
