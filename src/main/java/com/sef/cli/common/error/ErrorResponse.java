package com.sef.cli.common.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "統一錯誤回應 envelope")
public record ErrorResponse(
        @Schema(description = "HTTP status code") int code,
        @Schema(description = "User-friendly 訊息") String message,
        @Schema(description = "短 trace ID，配對 server log 用") String traceId
) {
}
