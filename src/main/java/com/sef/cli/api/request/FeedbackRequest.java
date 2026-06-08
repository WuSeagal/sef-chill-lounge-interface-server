package com.sef.cli.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeedbackRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private String username;
}
