package com.sef.cli.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "google-oauth")
public class GoogleOauthProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
}
