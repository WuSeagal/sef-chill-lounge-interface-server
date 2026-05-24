package com.sef.cli.testutil;

import com.sef.cli.user.entity.AdminUserEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockAdminSecurityContextFactory implements WithSecurityContextFactory<WithMockAdmin> {

    @Override
    public SecurityContext createSecurityContext(WithMockAdmin annotation) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        AdminUserEntity user = AdminUserEntity.builder()
                .providerUserId(annotation.providerUserId())
                .email(annotation.email())
                .googleName(annotation.googleName())
                .enabled(true)
                .firstLogin(false)
                .banned(false)
                .roleName(annotation.roleName())
                .build();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority(annotation.roleName())));
        ctx.setAuthentication(auth);
        return ctx;
    }
}
