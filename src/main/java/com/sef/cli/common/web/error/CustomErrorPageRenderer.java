package com.sef.cli.common.web.error;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class CustomErrorPageRenderer {

    private final String frontendBaseUrl;

    public CustomErrorPageRenderer(@Value("${frontend.base-url}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl == null ? "" : frontendBaseUrl.replaceAll("/+$", "");
    }

    public String render(int status, String path, String contextPath) {
        String cp = contextPath == null ? "" : contextPath;
        ErrorCopy copy = copyFor(status);
        return """
                <!doctype html>
                <html lang="zh-Hant">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>%s | SEF Chill Lounge</title>
                    <link rel="stylesheet" href="%s/error-page/error-page.css">
                </head>
                <body>
                    <main class="error-page">
                        <div class="error-page__card">
                            <img class="error-page__hero" src="%s/error-fallback.svg" alt="">
                            <h1 class="error-page__title">%s</h1>
                            <p class="error-page__subtitle">%s</p>
                            <p class="error-page__code">CODE %s</p>
                            <p class="error-page__from">%s</p>
                            <a class="error-page__cta" href="%s/">回到聊天</a>
                        </div>
                    </main>
                </body>
                </html>
                """.formatted(
                HtmlUtils.htmlEscape(copy.title()),
                cp,
                cp,
                HtmlUtils.htmlEscape(copy.title()),
                HtmlUtils.htmlEscape(copy.subtitle()),
                status,
                HtmlUtils.htmlEscape(normalizePath(path)),
                frontendBaseUrl
        );
    }

    private ErrorCopy copyFor(int status) {
        if (status == 401) {
            return new ErrorCopy("請先登入", "你需要登入才能查看這個頁面。");
        }
        if (status == 403) {
            return new ErrorCopy("沒有權限", "你目前不能查看這個頁面。");
        }
        if (status == 400 || status == 405 || status == 415) {
            return new ErrorCopy("請求格式不正確", "伺服器無法處理這個請求，請檢查網址、方法與內容格式。");
        }
        if (status >= 500) {
            return new ErrorCopy("伺服器錯誤", "伺服器在處理這個請求時發生問題，請稍後再試。");
        }
        return new ErrorCopy("找不到頁面", "你要找的內容不存在，或目前沒有對應的入口。");
    }

    private String normalizePath(String path) {
        return (path == null || path.isBlank()) ? "/" : path;
    }

    private record ErrorCopy(String title, String subtitle) {
    }
}
