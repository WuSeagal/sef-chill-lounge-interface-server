package com.sef.cli.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddSocialLinkRequest {
    private String platform;
    private String links;
}
