package com.sef.cli.common.web.error;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CustomErrorPageRendererTest {

    private final CustomErrorPageRenderer renderer = new CustomErrorPageRenderer("http://localhost:9045");

    @Test
    void renders404WithNotFoundCopyAndCta() {
        String html = renderer.render(404, "/foo/bar", "");
        assertThat(html).contains("找不到頁面");
        assertThat(html).contains("CODE 404");
        assertThat(html).contains("回到聊天");
        // CTA 連到前端 URL 的根，而非後端自己
        assertThat(html).contains("href=\"http://localhost:9045/\"");
        assertThat(html).contains("/error-page/error-page.css");
    }

    @Test
    void renders500WithServerCopy() {
        String html = renderer.render(500, "/x", "");
        assertThat(html).contains("伺服器錯誤");
        assertThat(html).contains("CODE 500");
    }

    @Test
    void renders403WithForbiddenCopy() {
        String html = renderer.render(403, "/x", "");
        assertThat(html).contains("沒有權限");
    }

    @Test
    void renders401WithLoginCopy() {
        String html = renderer.render(401, "/messages", "");
        assertThat(html).contains("請先登入");
        assertThat(html).contains("CODE 401");
        assertThat(html).contains("回到聊天");
    }

    @Test
    void renders400WithBadRequestCopy() {
        String html = renderer.render(400, "/x", "");
        assertThat(html).contains("請求格式不正確");
        assertThat(html).contains("CODE 400");
    }

    @Test
    void renders405WithBadRequestCopy() {
        String html = renderer.render(405, "/x", "");
        assertThat(html).contains("請求格式不正確");
        assertThat(html).contains("CODE 405");
    }

    @Test
    void renders415WithBadRequestCopy() {
        String html = renderer.render(415, "/x", "");
        assertThat(html).contains("請求格式不正確");
        assertThat(html).contains("CODE 415");
    }

    @Test
    void prefixesContextPathOnBackendAssetsOnly() {
        String html = renderer.render(404, "/x", "/sef-cli");
        assertThat(html).contains("/sef-cli/error-page/error-page.css");
        assertThat(html).contains("/sef-cli/error-fallback.svg");
        // CTA 連到前端 URL（與後端 context path 無關）
        assertThat(html).contains("href=\"http://localhost:9045/\"");
    }

    @Test
    void escapesPathToPreventInjection() {
        String html = renderer.render(404, "/x\"><script>alert(1)</script>", "");
        assertThat(html).doesNotContain("<script>alert(1)</script>");
    }
}
