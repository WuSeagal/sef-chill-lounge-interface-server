package com.sef.cli.user.web;

import ch.qos.logback.classic.Level;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.sef.cli.common.GoogleOAuthUtils;
import com.sef.cli.testutil.LogCaptor;
import com.sef.cli.user.entity.AdminUserEntity;
import com.sef.cli.user.repository.AdminUserRepository;
import com.sef.cli.user.web.map.AuthDtoMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * AuthController 行為 log 斷言（section 2）。GoogleOAuthUtils 為 static utility，以 mockStatic 控制其回傳。
 */
class AuthControllerLogTest {

    private final AdminUserRepository adminUserRepository = mock(AdminUserRepository.class);
    private final AuthDtoMapper authDtoMapper = mock(AuthDtoMapper.class);
    private final AuthController controller = new AuthController(adminUserRepository, authDtoMapper);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private GoogleIdToken.Payload payload(String sub, String email, String name) {
        GoogleIdToken.Payload p = new GoogleIdToken.Payload();
        p.setSubject(sub);
        p.setEmail(email);
        p.set("name", name);
        return p;
    }

    private HttpServletRequest requestWithNewSession() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);
        when(request.getSession(true)).thenReturn(mock(HttpSession.class));
        return request;
    }

    @Test
    void login_existingUser_logsInfoWithNewUserFalse() {
        try (LogCaptor captor = LogCaptor.forClass(AuthController.class);
             MockedStatic<GoogleOAuthUtils> oauth = mockStatic(GoogleOAuthUtils.class)) {
            GoogleTokenResponse tokenResponse = mock(GoogleTokenResponse.class);
            when(tokenResponse.getIdToken()).thenReturn("idtok");
            oauth.when(() -> GoogleOAuthUtils.getTokenByCode("code-1")).thenReturn(tokenResponse);
            oauth.when(() -> GoogleOAuthUtils.verifyIdToken("idtok"))
                    .thenReturn(payload("sub-1", "a@b.com", "Alice"));
            when(adminUserRepository.findByProviderUserId("sub-1"))
                    .thenReturn(Optional.of(AdminUserEntity.builder()
                            .providerUserId("sub-1").email("old@b.com").roleName("ROLE_USER").build()));
            when(adminUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            controller.googleAuthLogin(requestWithNewSession(), Map.of("code", "code-1"));

            captor.assertLogged(Level.INFO, "[LOGIN]", "userId=sub-1", "newUser=false");
        }
    }

    @Test
    void login_newUser_logsInfoWithNewUserTrue() {
        try (LogCaptor captor = LogCaptor.forClass(AuthController.class);
             MockedStatic<GoogleOAuthUtils> oauth = mockStatic(GoogleOAuthUtils.class)) {
            GoogleTokenResponse tokenResponse = mock(GoogleTokenResponse.class);
            when(tokenResponse.getIdToken()).thenReturn("idtok");
            oauth.when(() -> GoogleOAuthUtils.getTokenByCode("code-2")).thenReturn(tokenResponse);
            oauth.when(() -> GoogleOAuthUtils.verifyIdToken("idtok"))
                    .thenReturn(payload("sub-2", "new@b.com", "Bob"));
            when(adminUserRepository.findByProviderUserId("sub-2")).thenReturn(Optional.empty());
            when(adminUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            controller.googleAuthLogin(requestWithNewSession(), Map.of("code", "code-2"));

            captor.assertLogged(Level.INFO, "[LOGIN]", "userId=sub-2", "newUser=true");
        }
    }

    @Test
    void login_tokenException_logsErrorWithThrowable() {
        try (LogCaptor captor = LogCaptor.forClass(AuthController.class);
             MockedStatic<GoogleOAuthUtils> oauth = mockStatic(GoogleOAuthUtils.class)) {
            oauth.when(() -> GoogleOAuthUtils.getTokenByCode("bad"))
                    .thenThrow(new IOException("token exchange failed"));

            controller.googleAuthLogin(mock(HttpServletRequest.class), Map.of("code", "bad"));

            captor.assertLoggedWithThrowable(Level.ERROR, "[LOGIN_FAIL]");
        }
    }

    @Test
    void login_verificationReturnsNull_logsWarn() {
        try (LogCaptor captor = LogCaptor.forClass(AuthController.class);
             MockedStatic<GoogleOAuthUtils> oauth = mockStatic(GoogleOAuthUtils.class)) {
            GoogleTokenResponse tokenResponse = mock(GoogleTokenResponse.class);
            when(tokenResponse.getIdToken()).thenReturn("idtok");
            oauth.when(() -> GoogleOAuthUtils.getTokenByCode("code-3")).thenReturn(tokenResponse);
            oauth.when(() -> GoogleOAuthUtils.verifyIdToken("idtok")).thenReturn(null);

            controller.googleAuthLogin(mock(HttpServletRequest.class), Map.of("code", "code-3"));

            captor.assertLogged(Level.WARN, "[LOGIN_FAIL]");
        }
    }

    @Test
    void logout_logsInfoWithUserId() {
        AdminUserEntity user = AdminUserEntity.builder()
                .providerUserId("sub-9").email("c@b.com").roleName("ROLE_USER").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        try (LogCaptor captor = LogCaptor.forClass(AuthController.class)) {
            controller.logOutAuth(request, mock(HttpServletResponse.class));

            captor.assertLogged(Level.INFO, "[LOGOUT]", "userId=sub-9");
        }
    }
}
