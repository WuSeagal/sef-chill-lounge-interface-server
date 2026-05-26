package com.sef.cli.image.web;

import com.sef.cli.image.service.ChatImageUploadService;
import com.sef.cli.image.web.dto.ChatImageUploadResponse;
import com.sef.cli.image.web.exception.PayloadTooLargeException;
import com.sef.cli.image.web.exception.UnsupportedMediaTypeException;
import com.sef.cli.testutil.WithMockAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
class ChatImageControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    private ChatImageUploadService uploadService;

    @Test
    @WithMockAdmin(providerUserId = "user-1")
    void uploadsReturns201WithFileNameAndUrl() throws Exception {
        when(uploadService.upload(any(), eq("user-1")))
                .thenReturn(new ChatImageUploadResponse("1abcd-260526143000-x7K.png", "/image/1abcd-260526143000-x7K.png"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "a.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mvc.perform(multipart("/upload/chat-image").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileName").value("1abcd-260526143000-x7K.png"))
                .andExpect(jsonPath("$.data.url").value("/image/1abcd-260526143000-x7K.png"));
    }

    @Test
    void unauthenticatedReturns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mvc.perform(multipart("/upload/chat-image").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockAdmin(providerUserId = "user-1")
    void returns413OnPayloadTooLarge() throws Exception {
        when(uploadService.upload(any(), eq("user-1")))
                .thenThrow(new PayloadTooLargeException("file_too_large", 10));

        MockMultipartFile file = new MockMultipartFile(
                "file", "big.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1});

        mvc.perform(multipart("/upload/chat-image").file(file))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value("file_too_large"))
                .andExpect(jsonPath("$.data.maxSizeMB").value(10));
    }

    @Test
    @WithMockAdmin(providerUserId = "user-1")
    void returns415OnUnsupportedMedia() throws Exception {
        when(uploadService.upload(any(), eq("user-1")))
                .thenThrow(new UnsupportedMediaTypeException("unsupported_image_type"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.exe", MediaType.IMAGE_JPEG_VALUE, new byte[]{1});

        mvc.perform(multipart("/upload/chat-image").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("unsupported_image_type"));
    }
}
