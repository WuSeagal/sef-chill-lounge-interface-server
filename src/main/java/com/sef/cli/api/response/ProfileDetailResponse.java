package com.sef.cli.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDetailResponse {
    private String userId;
    private String username;
    private String furName;
    private String avatar;
    private String avatarColor;
    private String topicId;
    private TopicResponse topic;
    private List<TagResponse> tags;
    private List<SocialResponse> socials;
    private List<StickerResponse> stickers;
}
