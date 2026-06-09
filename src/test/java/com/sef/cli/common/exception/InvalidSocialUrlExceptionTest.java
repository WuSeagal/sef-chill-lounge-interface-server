package com.sef.cli.common.exception;

import com.sef.cli.attendee.service.SocialUrlValidator.Result;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InvalidSocialUrlExceptionTest {

    @Test
    void errorCodeMapsByReason() {
        assertThat(new InvalidSocialUrlException(Result.INVALID_URL).getErrorCode()).isEqualTo("invalid_social_url");
        assertThat(new InvalidSocialUrlException(Result.UNSAFE_URL).getErrorCode()).isEqualTo("unsafe_social_url");
        assertThat(new InvalidSocialUrlException(Result.PLATFORM_MISMATCH).getErrorCode()).isEqualTo("social_url_platform_mismatch");
        assertThat(new InvalidSocialUrlException(Result.TOO_LONG).getErrorCode()).isEqualTo("social_url_too_long");
    }

    @Test
    void carriesReason() {
        assertThat(new InvalidSocialUrlException(Result.TOO_LONG).getReason()).isEqualTo(Result.TOO_LONG);
    }
}
