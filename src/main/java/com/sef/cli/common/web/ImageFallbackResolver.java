package com.sef.cli.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.util.List;

public class ImageFallbackResolver implements ResourceResolver {

    public static final String FALLBACK_CLASSPATH = "static/error-fallback.svg";

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
        Resource fallback = new ClassPathResource(FALLBACK_CLASSPATH);
        return fallback.exists() ? fallback : null;
    }

    @Override
    @Nullable
    public String resolveUrlPath(
            @NonNull String resourcePath,
            @NonNull List<? extends Resource> locations,
            @NonNull ResourceResolverChain chain) {
        return chain.resolveUrlPath(resourcePath, locations);
    }
}
