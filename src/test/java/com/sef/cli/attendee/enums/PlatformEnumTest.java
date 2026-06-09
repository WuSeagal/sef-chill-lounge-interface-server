package com.sef.cli.attendee.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PlatformEnumTest {

    @Test
    void hasExactly16Values() {
        assertThat(PlatformEnum.values()).hasSize(16);
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
    void newVariantsHaveHostPatterns() {
        assertThat(PlatformEnum.FACEBOOK_PAGE.hasHostPattern()).isTrue();
        assertThat(PlatformEnum.DISCORD_SERVER.hasHostPattern()).isTrue();
        // FACEBOOK_PAGE 沿用 facebook host pattern
        assertThat(PlatformEnum.FACEBOOK_PAGE.getUrlHostPattern().matcher("www.facebook.com").matches()).isTrue();
        // DISCORD_SERVER 接受 discord.gg
        assertThat(PlatformEnum.DISCORD_SERVER.getUrlHostPattern().matcher("discord.gg").matches()).isTrue();
        // DISCORD 個人仍接受 discord.com（pattern 不變）
        assertThat(PlatformEnum.DISCORD.getUrlHostPattern().matcher("discord.com").matches()).isTrue();
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
