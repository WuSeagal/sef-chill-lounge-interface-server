package com.sef.cli.attendee.service;

import com.sef.cli.attendee.enums.PlatformEnum;
import com.sef.cli.attendee.service.SocialUrlValidator.Result;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SocialUrlValidatorTest {

    private final SocialUrlValidator validator = new SocialUrlValidator();

    @Test
    void httpsValidBrandUrlIsOk() {
        assertThat(validator.validate(PlatformEnum.X, "https://x.com/maomao")).isEqualTo(Result.OK);
        assertThat(validator.validate(PlatformEnum.X, "https://twitter.com/maomao")).isEqualTo(Result.OK);
    }

    @Test
    void nonHttpSchemeIsUnsafe() {
        assertThat(validator.validate(PlatformEnum.PERSONAL, "ftp://example.com")).isEqualTo(Result.UNSAFE_URL);
        assertThat(validator.validate(PlatformEnum.PERSONAL, "javascript:alert(1)")).isEqualTo(Result.UNSAFE_URL);
    }

    @Test
    void localhostAndIpAreUnsafe() {
        assertThat(validator.validate(PlatformEnum.PERSONAL, "http://localhost:3000")).isEqualTo(Result.UNSAFE_URL);
        assertThat(validator.validate(PlatformEnum.PERSONAL, "http://127.0.0.1")).isEqualTo(Result.UNSAFE_URL);
        assertThat(validator.validate(PlatformEnum.PERSONAL, "https://192.168.0.10/x")).isEqualTo(Result.UNSAFE_URL);
    }

    @Test
    void platformMismatchDetected() {
        assertThat(validator.validate(PlatformEnum.GITHUB, "https://x.com/maomao")).isEqualTo(Result.PLATFORM_MISMATCH);
    }

    @Test
    void personalAcceptsAnySafeUrl() {
        assertThat(validator.validate(PlatformEnum.PERSONAL, "https://my-portfolio.example.com")).isEqualTo(Result.OK);
    }

    @Test
    void safeLayerRunsBeforePlatformLayer() {
        assertThat(validator.validate(PlatformEnum.PERSONAL, "http://localhost")).isEqualTo(Result.UNSAFE_URL);
    }
}
