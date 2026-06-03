package com.sef.cli.attendee.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PlatformEnumTest {

    @Test
    void hasExactly14Values() {
        assertThat(PlatformEnum.values()).hasSize(14);
    }

    @Test
    void personalAndOtherHaveNoPattern() {
        assertThat(PlatformEnum.PERSONAL.hasHostPattern()).isFalse();
        assertThat(PlatformEnum.OTHER.hasHostPattern()).isFalse();
    }

    @Test
    void brandedPlatformsHavePattern() {
        assertThat(PlatformEnum.GITHUB.hasHostPattern()).isTrue();
        assertThat(PlatformEnum.X.hasHostPattern()).isTrue();
    }

    @Test
    void patternMatchesDocumentedHosts() {
        assertThat(PlatformEnum.GITHUB.getUrlHostPattern().matcher("github.com").matches()).isTrue();
        assertThat(PlatformEnum.GITHUB.getUrlHostPattern().matcher("x.com").matches()).isFalse();
        assertThat(PlatformEnum.X.getUrlHostPattern().matcher("twitter.com").matches()).isTrue();
        assertThat(PlatformEnum.THREADS.getUrlHostPattern().matcher("threads.com").matches()).isTrue();
        assertThat(PlatformEnum.THREADS.getUrlHostPattern().matcher("threads.net").matches()).isTrue();
        assertThat(PlatformEnum.STEAM.getUrlHostPattern().matcher("www.steamcommunity.com").matches()).isTrue();
    }
}
