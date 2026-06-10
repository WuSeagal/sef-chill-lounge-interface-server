package com.sef.cli.user.web;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.sef.cli.api.AuthApi;
import com.sef.cli.api.response.AuthResponse;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.common.GoogleOAuthUtils;
import com.sef.cli.user.entity.AdminUserEntity;
import com.sef.cli.user.repository.AdminUserRepository;
import com.sef.cli.user.web.map.AuthDtoMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AdminUserRepository adminUserRepository;
    private final AuthDtoMapper authDtoMapper;

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> checkUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            Object principal = authentication.getPrincipal();

            if (principal instanceof AdminUserEntity user) {
                return ResponseEntity.ok(
                        ApiResponse.success(
                                AuthResponse.builder()
                                        .providerUserId(user.getProviderUserId())
                                        .email(user.getEmail())
                                        .googleName(user.getGoogleName())
                                        .firstLogin(user.getFirstLogin())
                                        .enabled(user.getEnabled())
                                        .build()));
            } else {
                return ResponseEntity.status(401).body(ApiResponse.fail(401, "找不到用戶！"));
            }

        } else {
            return ResponseEntity.status(401).body(ApiResponse.fail(401, "尚未登入！"));
        }
    }

    @Override
    public void logOutAuth(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = null;
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof AdminUserEntity user) {
                userId = user.getProviderUserId();
            }
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        log.info("[LOGOUT] 使用者登出, userId={}", userId);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    public ResponseEntity<ApiResponse<List<AuthResponse>>> getUsers() {
        List<AuthResponse> list;
        try {
            list = authDtoMapper.toResponse(adminUserRepository.findAll());
        } catch (Exception e) {
            log.error("getUsers 取使用者列表失敗, 錯誤: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(500, "取使用者列表失敗：" + e.getMessage()));
        }
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @Override
    public ResponseEntity<ApiResponse<String>> googleAuthLogin(HttpServletRequest request, @RequestBody Map<String, String> body) {
        String code = body.get("code");
        GoogleIdToken.Payload payload = null;

        try {
            GoogleTokenResponse tokenResponse = GoogleOAuthUtils.getTokenByCode(code);
            payload = GoogleOAuthUtils.verifyIdToken(tokenResponse.getIdToken());
        } catch (Exception e) {
            log.error("[LOGIN_FAIL] Google token 交換或驗證失敗, 錯誤: {}", e.getMessage(), e);
        }

        if (payload == null) {
            // 涵蓋「verify 回 null 但未拋例外」的情況；例外路徑已於上方 catch 記 error，此處 warn 為正常雙層紀錄
            log.warn("[LOGIN_FAIL] payload 取得失敗, code 驗證未通過");
            return ResponseEntity.ok(ApiResponse.fail(500, "payload取得失敗"));
        }

        String providerUserId = payload.getSubject();
        String email = payload.getEmail();
        String googleName = (String) payload.get("name");

        AdminUserEntity user = adminUserRepository.findByProviderUserId(providerUserId).orElse(null);
        boolean isNewUser = (user == null);
        if (!isNewUser) {
            user.setEmail(email);
            user.setGoogleName(googleName);
            adminUserRepository.save(user);
        } else {
            user = AdminUserEntity.builder()
                    .providerUserId(providerUserId)
                    .email(email)
                    .googleName(googleName)
                    .enabled(true)
                    .firstLogin(true)
                    .banned(false)
                    .roleName("ROLE_USER")
                    .build();
            adminUserRepository.save(user);
        }
        log.info("[LOGIN] 使用者登入成功, userId={}, email={}, newUser={}", providerUserId, email, isNewUser);

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(user.getRoleName()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, authorities);

        // 先清掉舊 session，避免 changeSessionId() 在手機瀏覽器（尤其是 Safari）
        // 無法正確更新 cookie，導致後續 request 仍帶舊 session ID 而被 403
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.getSession(true)
                .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        SecurityContextHolder.getContext());

        return ResponseEntity.ok(ApiResponse.success("登入成功: " + email + " / " + googleName));
    }
}
