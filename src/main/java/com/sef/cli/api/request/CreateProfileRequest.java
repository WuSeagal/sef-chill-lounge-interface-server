package com.sef.cli.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfileRequest {
    private String username;
    private String furName;
    private String avatar;
    private String avatarColor;
    private Boolean avatarBorder;
    private String topicId;
}
