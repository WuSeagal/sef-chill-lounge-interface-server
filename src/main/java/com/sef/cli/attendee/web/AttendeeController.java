package com.sef.cli.attendee.web;

import com.sef.cli.api.AttendeeApi;
import com.sef.cli.api.request.CreateProfileRequest;
import com.sef.cli.api.request.UpdateProfileRequest;
import com.sef.cli.api.response.ProfileDetailResponse;
import com.sef.cli.api.response.ProfileResponse;
import com.sef.cli.api.response.SocialResponse;
import com.sef.cli.api.response.StickerResponse;
import com.sef.cli.api.response.TagResponse;
import com.sef.cli.api.response.TopicResponse;
import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.service.AttendeeService;
import com.sef.cli.attendee.web.map.AttendeeDtoMapper;
import com.sef.cli.chat.event.ChatEnvelope;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.event.response.ProfileUpdatedPayload;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.common.BanGuard;
import com.sef.cli.topic.web.map.TopicDtoMapper;
import com.sef.cli.user.entity.AdminUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AttendeeController implements AttendeeApi {

    private final AttendeeService attendeeService;
    private final AttendeeDtoMapper attendeeDtoMapper;
    private final TopicDtoMapper topicDtoMapper;
    private final ChatBroadcastService chatBroadcastService;
    private final BanGuard banGuard;

    @Override
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile() {
        AttendeeDataEntity e = attendeeService.getProfileOrThrow(currentUserId());
        return ResponseEntity.ok(ApiResponse.success(attendeeDtoMapper.toProfileResponse(e)));
    }

    @Override
    public ResponseEntity<ApiResponse<ProfileResponse>> createMyProfile(CreateProfileRequest req) {
        String userId = currentUserId();
        banGuard.assertNotBanned(userId);
        AttendeeDataEntity e = attendeeService.createProfile(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(attendeeDtoMapper.toProfileResponse(e)));
    }

    @Override
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(UpdateProfileRequest req) {
        String userId = currentUserId();
        banGuard.assertNotBanned(userId);
        AttendeeDataEntity before = attendeeService.getProfileOrThrow(userId);
        String oldFurName = before.getFurName();
        String oldAvatar = before.getAvatar();
        String oldAvatarColor = before.getAvatarColor();
        boolean oldAvatarBorder = before.isAvatarBorder();

        AttendeeDataEntity e = attendeeService.updateProfile(userId, req);

        // 此四個顯示欄位須與前端消費端 patch 欄位一致：useChatMessages.ts / useDashboardFeed.ts。新增顯示欄位時三處需同步。
        boolean displayChanged = !Objects.equals(oldFurName, e.getFurName())
                || !Objects.equals(oldAvatar, e.getAvatar())
                || !Objects.equals(oldAvatarColor, e.getAvatarColor())
                || oldAvatarBorder != e.isAvatarBorder();
        if (displayChanged) {
            chatBroadcastService.broadcastToAll(new ChatEnvelope<>(
                    ChatEventType.PROFILE_UPDATED,
                    System.currentTimeMillis(),
                    new ProfileUpdatedPayload(e.getUserId(), e.getFurName(), e.getAvatar(), e.getAvatarColor(), e.isAvatarBorder())
            ));
        }
        return ResponseEntity.ok(ApiResponse.success(attendeeDtoMapper.toProfileResponse(e)));
    }

    @Override
    public ResponseEntity<ApiResponse<ProfileDetailResponse>> getProfileByUserId(String userId) {
        AttendeeService.ProfileDetail detail = attendeeService.loadDetail(userId);
        List<TagResponse> tags = detail.tags().stream()
                .map(attendeeDtoMapper::toTagResponse)
                .collect(Collectors.toList());
        List<SocialResponse> socials = detail.socials().stream()
                .map(attendeeDtoMapper::toSocialResponse)
                .collect(Collectors.toList());
        List<StickerResponse> stickers = detail.stickers().stream()
                .map(attendeeDtoMapper::toStickerResponse)
                .collect(Collectors.toList());
        ProfileDetailResponse data = ProfileDetailResponse.builder()
                .userId(detail.entity().getUserId())
                .username(detail.entity().getUsername())
                .furName(detail.entity().getFurName())
                .avatar(detail.entity().getAvatar())
                .avatarColor(detail.entity().getAvatarColor())
                .avatarBorder(detail.entity().isAvatarBorder())
                .topicId(detail.entity().getTopicId())
                .topic(detail.topic() == null ? null : topicDtoMapper.toResponse(detail.topic()))
                .tags(tags)
                .socials(socials)
                .stickers(stickers)
                .build();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Override
    public ResponseEntity<ApiResponse<TopicResponse>> redrawTopicCard() {
        String userId = currentUserId();
        banGuard.assertNotBanned(userId);
        TopicResponse data = topicDtoMapper.toResponse(attendeeService.redrawTopicCard(userId));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AdminUserEntity user)) {
            throw new InsufficientAuthenticationException("no user");
        }
        return user.getProviderUserId();
    }
}
