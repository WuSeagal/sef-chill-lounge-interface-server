package com.sef.cli.config;

import com.sef.cli.common.web.error.StyledAccessDeniedHandler;
import com.sef.cli.common.web.error.StyledAuthenticationEntryPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@Slf4j
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${app.base-url}")
    private String appDomain;

    @Value("${frontend.base-url}")
    private String frontendDomain;

    @Value("${server.servlet.session.cookie.name:SEFCLISESSIONID}")
    private String sessionCookieName;

    @Value("${server.servlet.session.cookie.secure:true}")
    private boolean secureCookie;

    private final StyledAuthenticationEntryPoint authenticationEntryPoint;
    private final StyledAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(StyledAuthenticationEntryPoint authenticationEntryPoint,
                          StyledAccessDeniedHandler accessDeniedHandler) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(sessionCookieName);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secureCookie);
        serializer.setSameSite("Lax");
        serializer.setCookieMaxAge(Integer.MAX_VALUE);
        serializer.setCookiePath("/");
        return serializer;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(requests -> requests
                        // OAuth / auth 公開路徑
                        .requestMatchers("/check-auth", "/logout", "/user/googleAuth").permitAll()
                        // Swagger / OpenAPI 公開
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // attendee profile / tag / social / topic-card endpoints 需登入
                        .requestMatchers("/user/profile", "/user/profile/**").authenticated()
                        .requestMatchers("/user/tags", "/user/tags/**").authenticated()
                        .requestMatchers("/user/social-links", "/user/social-links/**").authenticated()
                        .requestMatchers("/user/topic-card/**").authenticated()
                        // 公開列表 / 隨機 endpoints 需登入
                        .requestMatchers("/members").authenticated()
                        .requestMatchers("/messages", "/messages/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/announcement").authenticated()
                        .requestMatchers("/tags").authenticated()
                        .requestMatchers("/topics/**").authenticated()
                        // 既有
                        .requestMatchers("/ws/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/sef/**").authenticated()
                        // chat-image-upload
                        .requestMatchers(HttpMethod.POST, "/upload/**").authenticated()
                        // 意見回饋
                        .requestMatchers(HttpMethod.POST, "/feedback").authenticated()
                        // 公開圖片靜態資源
                        .requestMatchers(HttpMethod.GET, "/image/**", "/user/**", "/sticker/**").permitAll()
                        .anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of(appDomain, frontendDomain));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .build();
    }
}
