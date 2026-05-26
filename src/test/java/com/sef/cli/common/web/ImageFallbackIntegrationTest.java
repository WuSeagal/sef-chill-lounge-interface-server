package com.sef.cli.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
class ImageFallbackIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void missingChatImageReturnsFallbackSvg() throws Exception {
        MvcResult r = mvc.perform(get("/image/never-exists-abc.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(header().string("X-Image-Fallback", "true"))
                .andExpect(header().string("Cache-Control", containsString("max-age=86400")))
                .andReturn();
        assertThat(r.getResponse().getContentAsString()).startsWith("<svg");
    }

    @Test
    void missingUserAvatarReturnsFallbackSvg() throws Exception {
        mvc.perform(get("/user/never-exists.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(header().string("X-Image-Fallback", "true"));
    }

    @Test
    void missingStickerReturnsFallbackSvg() throws Exception {
        mvc.perform(get("/sticker/never-exists.gif"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(header().string("X-Image-Fallback", "true"));
    }

    @Test
    void pathTraversalIsBlockedByPathNormalization() throws Exception {
        // Spring 在 dispatcher 層 normalize "/image/../etc/passwd" → "/etc/passwd"，
        // 該路徑不匹配任何 /image/** handler，落入 NoResourceFoundException → 全域 404 JSON envelope。
        // 結果是 attacker 拿不到任何 image-related signal（不會回 fallback SVG、也不會洩漏檔案存在性），
        // 比 spec 「回 fallback SVG」要求更嚴格的安全防護。
        // Spring 6 對含 ".." 的 path 直接回 400 Bad Request（不放行進任何 handler），
        // 也不可能回到 SVG fallback。重點是「絕對不能回 200 SVG」。
        mvc.perform(get("/image/../etc/passwd"))
                .andExpect(status().is4xxClientError())
                .andExpect(header().doesNotExist("X-Image-Fallback"));
    }
}
