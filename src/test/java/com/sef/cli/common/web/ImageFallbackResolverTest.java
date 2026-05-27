package com.sef.cli.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageFallbackResolverTest {

    @Test
    void delegatesToChainWhenResourceExists() {
        Resource fake = mock(Resource.class);
        ResourceResolverChain chain = mock(ResourceResolverChain.class);
        when(chain.resolveResource(any(), any(), any())).thenReturn(fake);

        ImageFallbackResolver r = new ImageFallbackResolver();
        Resource result = r.resolveResource(null, "real.jpg", List.of(), chain);

        assertThat(result).isSameAs(fake);
    }

    @Test
    void returnsClasspathFallbackWhenChainReturnsNull() {
        ResourceResolverChain chain = mock(ResourceResolverChain.class);
        when(chain.resolveResource(any(), any(), any())).thenReturn(null);

        ImageFallbackResolver r = new ImageFallbackResolver();
        Resource result = r.resolveResource(null, "missing.jpg", List.of(), chain);

        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
        assertThat(result).isInstanceOf(ClassPathResource.class);
        assertThat(((ClassPathResource) result).getPath()).isEqualTo("static/error-fallback.svg");
    }

    @Test
    void fallbackResourceCarriesNoStoreAndFallbackHeadersAsHttpResource() {
        ResourceResolverChain chain = mock(ResourceResolverChain.class);
        when(chain.resolveResource(any(), any(), any())).thenReturn(null);

        ImageFallbackResolver r = new ImageFallbackResolver();
        Resource result = r.resolveResource(null, "missing.jpg", List.of(), chain);

        // 必須是 HttpResource，讓 ResourceHttpRequestHandler 在 body 寫出前套用 header（真實容器有效）
        assertThat(result).isInstanceOf(org.springframework.web.servlet.resource.HttpResource.class);
        org.springframework.http.HttpHeaders h =
                ((org.springframework.web.servlet.resource.HttpResource) result).getResponseHeaders();
        assertThat(h.getCacheControl()).contains("no-store");
        assertThat(h.getFirst("X-Image-Fallback")).isEqualTo("true");
    }

    @Test
    void resolveUrlPathDelegatesToChain() {
        ResourceResolverChain chain = mock(ResourceResolverChain.class);
        when(chain.resolveUrlPath(any(), any())).thenReturn(null);

        ImageFallbackResolver r = new ImageFallbackResolver();
        String url = r.resolveUrlPath("missing.jpg", List.of(), chain);
        assertThat(url).isNull();
    }
}
