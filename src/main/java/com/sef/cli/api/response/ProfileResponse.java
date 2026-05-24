package com.sef.cli.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private String userId;
    private String username;
    private String furName;
    private String avatar;
    private String avatarColor;
    private String topicId;
}
