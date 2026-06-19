package com.sef.cli.common;

import com.sef.cli.common.exception.ForbiddenException;
import com.sef.cli.user.entity.AdminUserEntity;
import com.sef.cli.user.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BanGuardTest {

    private AdminUserRepository adminUserRepository;
    private BanGuard banGuard;

    @BeforeEach
    void setUp() {
        adminUserRepository = mock(AdminUserRepository.class);
        banGuard = new BanGuard(adminUserRepository);
    }

    private AdminUserEntity user(String providerUserId, boolean banned) {
        return AdminUserEntity.builder()
                .providerUserId(providerUserId)
                .roleName("ROLE_USER")
                .enabled(true)
                .firstLogin(false)
                .banned(banned)
                .build();
    }

    @Test
    void isBanned_returnsTrue_whenUserBanned() {
        when(adminUserRepository.findByProviderUserId("u-1"))
                .thenReturn(Optional.of(user("u-1", true)));

        assertThat(banGuard.isBanned("u-1")).isTrue();
    }

    @Test
    void isBanned_returnsFalse_whenUserNotBanned() {
        when(adminUserRepository.findByProviderUserId("u-2"))
                .thenReturn(Optional.of(user("u-2", false)));

        assertThat(banGuard.isBanned("u-2")).isFalse();
    }

    @Test
    void isBanned_returnsFalse_whenUserNotFound() {
        when(adminUserRepository.findByProviderUserId("ghost"))
                .thenReturn(Optional.empty());

        assertThat(banGuard.isBanned("ghost")).isFalse();
    }

    @Test
    void isBanned_returnsFalse_whenProviderUserIdNull() {
        assertThat(banGuard.isBanned(null)).isFalse();
    }

    @Test
    void assertNotBanned_throwsForbidden_whenBanned() {
        when(adminUserRepository.findByProviderUserId("u-1"))
                .thenReturn(Optional.of(user("u-1", true)));

        assertThatThrownBy(() -> banGuard.assertNotBanned("u-1"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void assertNotBanned_passes_whenNotBanned() {
        when(adminUserRepository.findByProviderUserId("u-2"))
                .thenReturn(Optional.of(user("u-2", false)));

        assertThatCode(() -> banGuard.assertNotBanned("u-2")).doesNotThrowAnyException();
    }

    @Test
    void assertNotBanned_passes_whenUserNotFound() {
        when(adminUserRepository.findByProviderUserId("ghost"))
                .thenReturn(Optional.empty());

        assertThatCode(() -> banGuard.assertNotBanned("ghost")).doesNotThrowAnyException();
    }
}
