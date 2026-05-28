package com.sef.cli.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagResponse {
    private String tagId;
    private String type;
    private String content;
    private boolean isCustom;
}
