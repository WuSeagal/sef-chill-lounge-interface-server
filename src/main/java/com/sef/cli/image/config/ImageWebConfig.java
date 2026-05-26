package com.sef.cli.image.config;

import com.sef.cli.common.web.ImageFallbackResolver;
import com.sef.cli.image.properties.ImageStorageProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ImageStorageProperties.class)
public class ImageWebConfig implements WebMvcConfigurer {

    private final ImageStorageProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        ImageFallbackResolver fallback = new ImageFallbackResolver();
        CacheControl oneDay = CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic();

        registry.addResourceHandler(properties.getChat().getUrlPrefix() + "**")
                .addResourceLocations("file:" + properties.getBasePath() + "image/")
                .setCacheControl(oneDay)
                .resourceChain(true)
                .addResolver(fallback)
                .addResolver(new PathResourceResolver());

        registry.addResourceHandler(properties.getUser().getUrlPrefix() + "**")
                .addResourceLocations("file:" + properties.getBasePath() + "user/")
                .setCacheControl(oneDay)
                .resourceChain(true)
                .addResolver(fallback)
                .addResolver(new PathResourceResolver());

        registry.addResourceHandler(properties.getSticker().getUrlPrefix() + "**")
                .addResourceLocations("file:" + properties.getBasePath() + "sticker/")
                .setCacheControl(oneDay)
                .resourceChain(true)
                .addResolver(fallback)
                .addResolver(new PathResourceResolver());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        HandlerInterceptor headers = new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setHeader("X-Content-Type-Options", "nosniff");
                return true;
            }

            @Override
            public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mv) {
                String uri = request.getRequestURI();
                String ct = response.getContentType();
                if (ct != null && ct.startsWith("image/svg+xml") && !uri.toLowerCase().endsWith(".svg")) {
                    response.setHeader("X-Image-Fallback", "true");
                }
            }
        };
        registry.addInterceptor(headers)
                .addPathPatterns(
                        properties.getChat().getUrlPrefix() + "**",
                        properties.getUser().getUrlPrefix() + "**",
                        properties.getSticker().getUrlPrefix() + "**"
                );
    }
}
