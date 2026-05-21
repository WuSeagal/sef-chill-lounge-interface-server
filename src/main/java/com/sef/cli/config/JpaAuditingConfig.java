package com.sef.cli.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider", auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean(name = "auditingDateTimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now(ZoneOffset.ofHours(8)));
    }

    @Bean(name = "auditorAware")
    public AuditorAware<String> auditorAware() {
        return () -> {
            // 從 Spring Security 的 Context 中獲取當前認證資訊
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 檢查是否為空、是否已認證，或者是否為匿名使用者 (尚未登入的情況)
            if (authentication == null ||
                    !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {

                // 尚未登入或系統自動執行的操作，給予一個預設值，例如 "system"
                return Optional.of("system");
            }

            // 回傳當前登入使用者的名稱 (通常是帳號或 username)
            return Optional.of(authentication.getName());
        };
    }
}
