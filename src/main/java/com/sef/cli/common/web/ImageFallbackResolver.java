package com.sef.cli.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.resource.HttpResource;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.util.List;

public class ImageFallbackResolver implements ResourceResolver {

    public static final String FALLBACK_CLASSPATH = "static/error-fallback.svg";

    private static final Resource FALLBACK_RESOURCE = new FallbackImageResource(FALLBACK_CLASSPATH);

    @Override
    @Nullable
    public Resource resolveResource(
            @Nullable HttpServletRequest request,
            @NonNull String requestPath,
            @NonNull List<? extends Resource> locations,
            @NonNull ResourceResolverChain chain) {
        Resource found = chain.resolveResource(request, requestPath, locations);
        if (found != null) {
            return found;
        }
        return FALLBACK_RESOURCE.exists() ? FALLBACK_RESOURCE : null;
    }

    @Override
    @Nullable
    public String resolveUrlPath(
            @NonNull String resourcePath,
            @NonNull List<? extends Resource> locations,
            @NonNull ResourceResolverChain chain) {
        return chain.resolveUrlPath(resourcePath, locations);
    }

    /**
     * 以 {@link HttpResource} 攜帶回應 header，讓 {@code ResourceHttpRequestHandler.setHeaders}
     * 在 body 寫出前套用（postHandle 太晚——真實容器中 response 已 commit，setHeader 會被忽略）。
     */
    static final class FallbackImageResource extends ClassPathResource implements HttpResource {

        FallbackImageResource(String path) {
            super(path);
        }

        @Override
        public HttpHeaders getResponseHeaders() {
            HttpHeaders headers = new HttpHeaders();
            // fallback 是臨時頂替，禁止快取（避免上傳真檔後仍回舊 SVG）
            headers.setCacheControl("no-store, max-age=0");
            headers.set("X-Image-Fallback", "true");
            return headers;
        }
    }
}
