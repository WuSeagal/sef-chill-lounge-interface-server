package com.sef.cli.common.web.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class StyledAccessDeniedHandlerTest {

    private StyledAccessDeniedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StyledAccessDeniedHandler(
                new CustomErrorPageRenderer("http://localhost:9045"),
                new ObjectMapper()
        );
    }

    @Test
    void browserAccept_writesStyledHtml403() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        req.addHeader("Accept", "text/html");
        MockHttpServletResponse res = new MockHttpServletResponse();

        handler.handle(req, res, new AccessDeniedException("denied"));

        assertThat(res.getStatus()).isEqualTo(403);
        assertThat(res.getContentType()).startsWith("text/html");
        assertThat(res.getContentAsString()).contains("沒有權限");
        assertThat(res.getContentAsString()).contains("CODE 403");
        assertThat(res.getContentAsString()).contains("回到聊天");
    }

    @Test
    void jsonAccept_writesJsonEnvelope403() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        req.addHeader("Accept", "application/json");
        MockHttpServletResponse res = new MockHttpServletResponse();

        handler.handle(req, res, new AccessDeniedException("denied"));

        assertThat(res.getStatus()).isEqualTo(403);
        assertThat(res.getContentType()).startsWith("application/json");
        String body = res.getContentAsString();
        assertThat(body).contains("\"code\":403");
        assertThat(body).contains("\"message\":\"forbidden\"");
    }

    @Test
    void noAccept_writesJsonEnvelope403() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse res = new MockHttpServletResponse();

        handler.handle(req, res, new AccessDeniedException("denied"));

        assertThat(res.getStatus()).isEqualTo(403);
        assertThat(res.getContentType()).startsWith("application/json");
    }

    @Test
    void wildcardAccept_writesJsonEnvelope403() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        req.addHeader("Accept", "*/*");
        MockHttpServletResponse res = new MockHttpServletResponse();

        handler.handle(req, res, new AccessDeniedException("denied"));

        assertThat(res.getStatus()).isEqualTo(403);
        assertThat(res.getContentType()).startsWith("application/json");
    }
}
