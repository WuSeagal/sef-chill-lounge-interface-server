package com.sef.cli.testutil;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 測試用：注入一個 fake AdminUserEntity 到 SecurityContext，模擬已登入使用者。
 *
 * <p>用法：在 test method 上加 {@code @WithMockAdmin(providerUserId = "u-1")}。
 * 之後 controller 內 {@code SecurityContextHolder.getContext().getAuthentication().getPrincipal()}
 * 會拿到 AdminUserEntity (providerUserId = "u-1")。
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockAdminSecurityContextFactory.class)
public @interface WithMockAdmin {
    String providerUserId() default "u-test";

    String email() default "test@example.com";

    String googleName() default "Test User";

    String roleName() default "ROLE_USER";
}
