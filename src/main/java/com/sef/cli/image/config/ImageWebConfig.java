package com.sef.cli.image.config;

import com.sef.cli.image.properties.ImageStorageProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ImageStorageProperties.class)
public class ImageWebConfig implements WebMvcConfigurer {

    private final ImageStorageProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(properties.getChat().getUrlPrefix() + "**")
                .addResourceLocations("file:" + properties.getBasePath() + "image/");
        registry.addResourceHandler(properties.getUser().getUrlPrefix() + "**")
                .addResourceLocations("file:" + properties.getBasePath() + "user/");
        registry.addResourceHandler(properties.getSticker().getUrlPrefix() + "**")
                .addResourceLocations("file:" + properties.getBasePath() + "sticker/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        HandlerInterceptor nosniff = new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setHeader("X-Content-Type-Options", "nosniff");
                return true;
            }
        };
        registry.addInterceptor(nosniff)
                .addPathPatterns(
                        properties.getChat().getUrlPrefix() + "**",
                        properties.getUser().getUrlPrefix() + "**",
                        properties.getSticker().getUrlPrefix() + "**"
                );
    }
}
