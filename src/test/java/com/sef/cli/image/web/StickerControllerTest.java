package com.sef.cli.image.web;

import com.sef.cli.api.response.StickerResponse;
import com.sef.cli.image.service.StickerUploadService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
class StickerControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    private StickerUploadService stickerUploadService;

    @Test
    @WithMockAdmin(providerUserId = "u-1")
    void uploadStickerReturnsCreated() throws Exception {
        when(stickerUploadService.upload(any(), eq("u-1"), eq(1)))
                .thenReturn(StickerResponse.builder().id(1L).stickerNo(1).sticker("/sticker/u-1/1.png?v=1").build());
        MockMultipartFile file = new MockMultipartFile("file", "s.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mvc.perform(multipart("/upload/sticker/1").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.stickerNo").value(1))
                .andExpect(jsonPath("$.data.sticker").value("/sticker/u-1/1.png?v=1"));
    }

    @Test
    void uploadStickerRequiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "s.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});
        mvc.perform(multipart("/upload/sticker/1").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockAdmin(providerUserId = "u-1")
    void deleteStickerReturnsOk() throws Exception {
        mvc.perform(delete("/upload/sticker/2"))
                .andExpect(status().isOk());
    }
}
