package com.sef.cli.user.web;

import com.sef.cli.api.response.AuthResponse;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.user.entity.AdminUserEntity;
import com.sef.cli.user.repository.AdminUserRepository;
import com.sef.cli.user.web.map.AuthDtoMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * check-auth 須即時反映 DB 的 banned（design.md D2 / backend-auth-layer spec）：
 * 不可只看登入當下序列化於 session 的 principal 快照。
 */
class AuthControllerCheckAuthTest {

    private final AdminUserRepository adminUserRepository = mock(AdminUserRepository.class);
    private final AuthDtoMapper authDtoMapper = mock(AuthDtoMapper.class);
    private final AuthController controller = new AuthController(adminUserRepository, authDtoMapper);

    @BeforeEach
    void clearBefore() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearAfter() {
        SecurityContextHolder.clearContext();
    }

    private void login(AdminUserEntity principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private AdminUserEntity user(String providerUserId, boolean banned) {
        return AdminUserEntity.builder()
                .providerUserId(providerUserId)
                .email("a@b.com")
                .googleName("Alice")
                .roleName("ROLE_USER")
                .enabled(true)
                .firstLogin(false)
                .banned(banned)
                .build();
    }

    @Test
    void checkUser_loggedIn_returnsBannedField() {
        AdminUserEntity principal = user("sub-1", false);
        login(principal);
        when(adminUserRepository.findByProviderUserId("sub-1")).thenReturn(Optional.of(principal));

        ResponseEntity<ApiResponse<AuthResponse>> resp = controller.checkUser();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        AuthResponse body = resp.getBody().getData();
        assertThat(body.getProviderUserId()).isEqualTo("sub-1");
        assertThat(body.getBanned()).isFalse();
    }

    @Test
    void checkUser_bannedAfterLogin_returns200WithBannedTrue() {
        // 登入快照 banned=false（principal），但 DB 當下 banned=true
        AdminUserEntity stalePrincipal = user("sub-2", false);
        login(stalePrincipal);
        when(adminUserRepository.findByProviderUserId("sub-2"))
                .thenReturn(Optional.of(user("sub-2", true)));

        ResponseEntity<ApiResponse<AuthResponse>> resp = controller.checkUser();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        AuthResponse body = resp.getBody().getData();
        assertThat(body.getBanned()).isTrue();
    }

    @Test
    void checkUser_dbRowMissing_returns200WithBannedFalse() {
        AdminUserEntity principal = user("sub-3", false);
        login(principal);
        when(adminUserRepository.findByProviderUserId("sub-3")).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<AuthResponse>> resp = controller.checkUser();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().getData().getBanned()).isFalse();
    }
}
