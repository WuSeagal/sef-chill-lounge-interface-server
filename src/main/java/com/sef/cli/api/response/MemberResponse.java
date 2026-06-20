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
public class MemberResponse {
    private String userId;
    private String username;
    private String furName;
    private String avatar;
    private String avatarColor;
    /** 該 attendee 的興趣 TAG（people-directory：供名單顯示「同好」線索）。無則為空陣列。 */
    private List<TagResponse> tags;
}
