package com.sef.cli.api.response;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class AuthResponse {
    private String providerUserId;
    private String email;
    private String googleName;
    private Boolean enabled;
    private Boolean firstLogin;
    private Boolean banned;
}
