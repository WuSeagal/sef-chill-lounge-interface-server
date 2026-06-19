package com.sef.cli.user.repository;

import com.sef.cli.config.JpaAuditingConfig;
import com.sef.cli.user.entity.AdminUserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AdminUserRepositoryTest {

    @Autowired
    private AdminUserRepository adminUserRepository;

    private AdminUserEntity newUser(String providerUserId, boolean banned) {
        return AdminUserEntity.builder()
                .providerUserId(providerUserId)
                .email(providerUserId + "@example.com")
                .googleName("Name-" + providerUserId)
                .roleName("ROLE_USER")
                .enabled(true)
                .firstLogin(false)
                .banned(banned)
                .build();
    }

    @Test
    void findByBannedTrue_returnsOnlyBannedUsers() {
        long initialBanned = adminUserRepository.findByBannedTrue().size();

        adminUserRepository.save(newUser("bantest-banned-1", true));
        adminUserRepository.save(newUser("bantest-banned-2", true));
        adminUserRepository.save(newUser("bantest-clean-1", false));

        List<AdminUserEntity> banned = adminUserRepository.findByBannedTrue();

        assertThat(banned).hasSize((int) initialBanned + 2);
        assertThat(banned)
                .extracting(AdminUserEntity::getProviderUserId)
                .contains("bantest-banned-1", "bantest-banned-2")
                .doesNotContain("bantest-clean-1");
        assertThat(banned).allMatch(AdminUserEntity::getBanned);
    }

    @Test
    void findByBannedTrue_excludesUnbannedUser() {
        adminUserRepository.save(newUser("bantest-clean-2", false));

        List<AdminUserEntity> banned = adminUserRepository.findByBannedTrue();

        assertThat(banned)
                .extracting(AdminUserEntity::getProviderUserId)
                .doesNotContain("bantest-clean-2");
    }
}
