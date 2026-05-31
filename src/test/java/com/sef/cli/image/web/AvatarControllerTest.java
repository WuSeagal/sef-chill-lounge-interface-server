package com.sef.cli.image.web;

import com.sef.cli.image.service.AvatarUploadService;
import com.sef.cli.image.web.dto.AvatarUploadResponse;
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
class AvatarControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    private AvatarUploadService avatarUploadService;

    @Test
    @WithMockAdmin(providerUserId = "google-123")
    void uploadAvatarReturnsCreatedPath() throws Exception {
        when(avatarUploadService.upload(any(), eq("google-123")))
                .thenReturn(new AvatarUploadResponse("/user/google-123.png?v=1748640000000"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "fox.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mvc.perform(multipart("/upload/avatar").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.avatarPath").value("/user/google-123.png?v=1748640000000"));
    }

    @Test
    void uploadAvatarRequiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fox.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mvc.perform(multipart("/upload/avatar").file(file))
                .andExpect(status().isUnauthorized());
    }
}
