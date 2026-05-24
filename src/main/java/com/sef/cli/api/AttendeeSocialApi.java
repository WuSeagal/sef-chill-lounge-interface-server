package com.sef.cli.api;

import com.sef.cli.api.request.AddSocialLinkRequest;
import com.sef.cli.api.request.RemoveSocialLinkRequest;
import com.sef.cli.api.response.SocialResponse;
import com.sef.cli.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Attendee Social Link API", description = "")
public interface AttendeeSocialApi {

    @Operation(summary = "新增社群連結", description = "")
    @PostMapping("/user/social-links")
    ResponseEntity<ApiResponse<SocialResponse>> addSocialLink(@RequestBody AddSocialLinkRequest req);

    @Operation(summary = "刪除自己的社群連結", description = "")
    @PostMapping("/user/social-links/remove")
    ResponseEntity<ApiResponse<Object>> removeSocialLink(@RequestBody RemoveSocialLinkRequest req);
}
