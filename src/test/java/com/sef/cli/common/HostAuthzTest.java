package com.sef.cli.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HostAuthzTest {

    @Test
    void hostProviderUserIdIsHost() {
        assertThat(HostAuthz.isHost("111427449810799428954")).isTrue();
    }

    @Test
    void constantMatchesHardcodedHostId() {
        assertThat(HostAuthz.HOST_PROVIDER_USER_ID).isEqualTo("111427449810799428954");
    }

    @Test
    void otherProviderUserIdIsNotHost() {
        assertThat(HostAuthz.isHost("999999999999999999999")).isFalse();
    }

    @Test
    void nullIsNotHost() {
        assertThat(HostAuthz.isHost(null)).isFalse();
    }
}
