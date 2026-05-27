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
        // fallback 是「臨時頂替」，不可被快取（否則上傳真檔後仍回 SVG）→ no-store
        MvcResult r = mvc.perform(get("/image/never-exists-abc.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(header().string("X-Image-Fallback", "true"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andReturn();
        String svg = r.getResponse().getContentAsString();
        assertThat(svg).startsWith("<svg");
        // fallback SVG 必須有明確 intrinsic width/height，否則在 <img> 的 width:auto 容器內會塌成 0×0
        assertThat(svg).contains("width=\"150\"");
        assertThat(svg).contains("height=\"150\"");
    }

    @Test
    void missingSvgFileStillSetsFallbackHeader() throws Exception {
        // 即使請求的副檔名是 .svg（avatar 可上傳 .svg），缺檔走 fallback 時仍須設 header；
        // 不可用「URI 結尾是否 .svg」推測，須由 resolver 主動標記 request attribute。
        mvc.perform(get("/user/never-exists.svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(header().string("X-Image-Fallback", "true"));
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
