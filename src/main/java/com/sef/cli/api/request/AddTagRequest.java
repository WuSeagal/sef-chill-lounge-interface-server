package com.sef.cli.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTagRequest {
    private String tagId;
    private String type;
    private String content;
}
