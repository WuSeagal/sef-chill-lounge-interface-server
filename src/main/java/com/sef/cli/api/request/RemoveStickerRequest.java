package com.sef.cli.api.request;

import lombok.Data;

/** 移除貼圖請求（api-delete-to-post：依專案 GET/POST-only 慣例，移除走 POST + @RequestBody）。 */
@Data
public class RemoveStickerRequest {

    private Long id;
}
