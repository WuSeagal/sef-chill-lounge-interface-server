package com.sef.cli.api;

import com.sef.cli.api.request.CreateProfileRequest;
import com.sef.cli.api.request.UpdateProfileRequest;
import com.sef.cli.api.response.ProfileDetailResponse;
import com.sef.cli.api.response.ProfileResponse;
import com.sef.cli.api.response.TopicResponse;
import com.sef.cli.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Attendee Profile API", description = "")
public interface AttendeeApi {

    @Operation(summary = "取得當前登入者 profile", description = "")
    @GetMapping("/user/profile")
    ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile();

    @Operation(summary = "初次建立 profile", description = "")
    @PostMapping("/user/profile")
    ResponseEntity<ApiResponse<ProfileResponse>> createMyProfile(@RequestBody CreateProfileRequest req);

    @Operation(summary = "更新自己 profile（部分欄位）", description = "")
    @PostMapping("/user/profile/update")
    ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(@RequestBody UpdateProfileRequest req);

    @Operation(summary = "取得指定使用者完整 profile（nested）", description = "")
    @GetMapping("/user/profile/{userId}")
    ResponseEntity<ApiResponse<ProfileDetailResponse>> getProfileByUserId(@PathVariable String userId);

    @Operation(summary = "重抽話題卡", description = "")
    @PostMapping("/user/topic-card/redraw")
    ResponseEntity<ApiResponse<TopicResponse>> redrawTopicCard();
}
