package com.sef.cli.common.web.error;

import com.sef.cli.common.ApiResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;

/**
 * 接管 /error，取代 Spring Boot 預設 BasicErrorController（實作 ErrorController 使其退場），
 * 消除 whitelabel / 容器預設錯誤頁外洩；依 Accept 分流瀏覽器 HTML 與 API JSON。
 */
@Controller
public class CustomErrorPageController implements ErrorController {

    private final CustomErrorPageRenderer renderer;

    public CustomErrorPageController(CustomErrorPageRenderer renderer) {
        this.renderer = renderer;
    }

    @RequestMapping(value = "/error", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> errorHtml(HttpServletRequest request) {
        int status = resolveStatus(request);
        return ResponseEntity.status(status)
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(renderer.render(status, resolvePath(request), request.getContextPath()));
    }

    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> errorJson(HttpServletRequest request) {
        int status = resolveStatus(request);
        return ResponseEntity.status(status).body(ApiResponse.fail(status, messageFor(status)));
    }

    private int resolveStatus(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        return (code instanceof Integer s) ? s : HttpStatus.NOT_FOUND.value();
    }

    private String resolvePath(HttpServletRequest request) {
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        return uri != null ? uri.toString() : request.getRequestURI();
    }

    private String messageFor(int status) {
        return status >= 500 ? "系統暫時無法處理您的請求，請稍後再試" : "找不到資源";
    }
}
