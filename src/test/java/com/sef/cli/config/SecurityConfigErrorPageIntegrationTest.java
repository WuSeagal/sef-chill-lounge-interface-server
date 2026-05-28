package com.sef.cli.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
class SecurityConfigErrorPageIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void unauthenticatedRequest_browser_returns401StyledHtml() throws Exception {
        mvc.perform(get("/messages").accept(MediaType.TEXT_HTML))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("請先登入")))
                .andExpect(content().string(containsString("CODE 401")))
                .andExpect(content().string(containsString("error-page.css")))
                .andExpect(content().string(containsString("回到聊天")));
    }

    @Test
    void unauthenticatedRequest_api_returns401JsonEnvelope() throws Exception {
        mvc.perform(get("/messages").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("unauthenticated"));
    }

    @Test
    void unauthenticatedRequest_noAccept_returns401Json() throws Exception {
        mvc.perform(get("/messages"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401));
    }
}
