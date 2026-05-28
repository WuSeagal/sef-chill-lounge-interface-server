package com.sef.cli.common.web.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StyledAuthenticationEntryPointTest {

    private StyledAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        CustomErrorPageRenderer renderer = new CustomErrorPageRenderer("http://localhost:9045");
        ObjectMapper objectMapper = new ObjectMapper();
        entryPoint = new StyledAuthenticationEntryPoint(renderer, objectMapper);
    }

    @Test
    void browserAccept_writesStyledHtml401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/messages");
        req.addHeader("Accept", "text/html");
        MockHttpServletResponse res = new MockHttpServletResponse();

        entryPoint.commence(req, res, mock(AuthenticationException.class));

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentType()).startsWith("text/html");
        assertThat(res.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        assertThat(res.getContentAsString()).contains("請先登入");
        assertThat(res.getContentAsString()).contains("CODE 401");
        assertThat(res.getContentAsString()).contains("回到聊天");
    }

    @Test
    void jsonAccept_writesJsonEnvelope401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/messages");
        req.addHeader("Accept", "application/json");
        MockHttpServletResponse res = new MockHttpServletResponse();

        entryPoint.commence(req, res, mock(AuthenticationException.class));

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentType()).startsWith("application/json");
        String body = res.getContentAsString();
        assertThat(body).contains("\"code\":401");
        assertThat(body).contains("\"message\":\"unauthenticated\"");
    }

    @Test
    void noAccept_writesJsonEnvelope401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/messages");
        MockHttpServletResponse res = new MockHttpServletResponse();

        entryPoint.commence(req, res, mock(AuthenticationException.class));

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentType()).startsWith("application/json");
    }

    @Test
    void wildcardAccept_writesJsonEnvelope401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/messages");
        req.addHeader("Accept", "*/*");
        MockHttpServletResponse res = new MockHttpServletResponse();

        entryPoint.commence(req, res, mock(AuthenticationException.class));

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentType()).startsWith("application/json");
    }

    @Test
    void responseAlreadyCommitted_returnsEarlyWithoutWriting() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/messages");
        req.addHeader("Accept", "text/html");
        MockHttpServletResponse res = new MockHttpServletResponse();
        res.flushBuffer();

        entryPoint.commence(req, res, mock(AuthenticationException.class));

        // response 已 committed,handler 不該再寫東西
        assertThat(res.getContentAsString()).isEmpty();
    }
}
