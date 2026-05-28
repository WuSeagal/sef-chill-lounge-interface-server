package com.sef.cli.common.web.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sef.cli.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class StyledAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final CustomErrorPageRenderer renderer;
    private final ObjectMapper objectMapper;

    public StyledAuthenticationEntryPoint(CustomErrorPageRenderer renderer, ObjectMapper objectMapper) {
        this.renderer = renderer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains(MediaType.TEXT_HTML_VALUE)) {
            response.setContentType(MediaType.TEXT_HTML_VALUE);
            response.getWriter().write(
                    renderer.render(401, request.getRequestURI(), request.getContextPath())
            );
        } else {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    objectMapper.writeValueAsString(ApiResponse.fail(401, "unauthenticated"))
            );
        }
    }
}
