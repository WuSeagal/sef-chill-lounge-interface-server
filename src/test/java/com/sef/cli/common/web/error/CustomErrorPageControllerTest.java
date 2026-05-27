package com.sef.cli.common.web.error;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.RequestDispatcher;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
class CustomErrorPageControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void errorPath_withHtmlAccept_returnsStyledHtmlNotWhitelabel() throws Exception {
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("error-page.css")))
                .andExpect(content().string(containsString("回到聊天")))
                // Spring 預設 whitelabel 頁的特徵字樣，不該出現
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Whitelabel"))));
    }

    @Test
    void errorPath_withJsonAccept_returnsJsonEnvelope() throws Exception {
        mvc.perform(get("/error").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void errorPath_withWildcardAccept_returnsJsonNotHtml() throws Exception {
        // 鎖定行為：*/*（curl 預設 / 無 Accept）的非瀏覽器 client 應拿 JSON，不該收到 HTML 頁
        mvc.perform(get("/error").accept(MediaType.ALL))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void errorPath_usesDispatchedStatusCodeForHtml() throws Exception {
        // 有 error dispatch 帶入的 status（500）時，HTML 應反映 500 與伺服器錯誤文案
        mvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("伺服器錯誤")))
                .andExpect(content().string(containsString("CODE 500")));
    }
}
