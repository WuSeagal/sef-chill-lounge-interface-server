package com.sef.cli.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void exposesCodeMessageTraceId() {
        ErrorResponse r = new ErrorResponse(500, "系統錯誤", "abc12345");
        assertThat(r.code()).isEqualTo(500);
        assertThat(r.message()).isEqualTo("系統錯誤");
        assertThat(r.traceId()).isEqualTo("abc12345");
    }

    @Test
    void serializesToJsonWithExpectedFields() throws Exception {
        ErrorResponse r = new ErrorResponse(404, "找不到資源", "xyz98765");
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(r);
        assertThat(json).contains("\"code\":404");
        assertThat(json).contains("\"message\":\"找不到資源\"");
        assertThat(json).contains("\"traceId\":\"xyz98765\"");
    }
}
