package com.sef.cli.image.config;

import com.sef.cli.common.web.ImageFallbackResolver;
import com.sef.cli.image.properties.ImageStorageProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.CachingResourceResolver;
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
        CacheControl immutableOneYear = CacheControl.maxAge(365, TimeUnit.DAYS)
                .cachePublic()
                .immutable();
        CacheControl longLivedOneYear = CacheControl.maxAge(365, TimeUnit.DAYS)
                .cachePublic();

        // fallback resolver 必須在快取鏈「外層」：CachingResourceResolver 只包住 PathResourceResolver，
        // 因此缺檔（null）不會進快取，fallback SVG 也不會被快取頂替後續真檔。各 handler 用獨立 cache，
        // 避免不同前綴下相同相對路徑（如 foo.jpg）的 cache key 互相覆蓋。
        registry.addResourceHandler(properties.getChat().getUrlPrefix() + "**")
                .addResourceLocations("file:" + properties.getBasePath() + "image/")
                .setCacheControl(immutableOneYear)
                .resourceChain(false)
                .addResolver(fallback)
                .addResolver(new CachingResourceResolver(new ConcurrentMapCache("sef-image-chat-cache")))
                .addResolver(new PathResourceResolver());

        registry.addResourceHandler(properties.getUser().getUrlPrefix() + "**")
                .addResourceLocations("file:" + properties.getBasePath() + "user/")
                .setCacheControl(longLivedOneYear)
                .resourceChain(false)
                .addResolver(fallback)
                .addResolver(new CachingResourceResolver(new ConcurrentMapCache("sef-image-user-cache")))
                .addResolver(new PathResourceResolver());

        registry.addResourceHandler(properties.getSticker().getUrlPrefix() + "**")
                .addResourceLocations("file:" + properties.getBasePath() + "sticker/")
                .setCacheControl(immutableOneYear)
                .resourceChain(false)
                .addResolver(fallback)
                .addResolver(new CachingResourceResolver(new ConcurrentMapCache("sef-image-sticker-cache")))
                .addResolver(new PathResourceResolver());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // X-Image-Fallback 與 fallback 的 no-store 改由 ImageFallbackResolver 回傳的 HttpResource 攜帶
        // （在 body 寫出前由 ResourceHttpRequestHandler.setHeaders 套用，真實容器有效）；
        // 此處只負責所有圖片回應共用的 nosniff。
        HandlerInterceptor headers = new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setHeader("X-Content-Type-Options", "nosniff");
                return true;
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
