package com.sef.cli.image.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
class ImageWebConfigIntegrationTest {

    @DynamicPropertySource
    static void overrideBasePathAndSeed(DynamicPropertyRegistry reg) throws IOException {
        Path tempBase = Files.createTempDirectory("sef-images-it-");
        Files.createDirectories(tempBase.resolve("image"));
        Files.createDirectories(tempBase.resolve("user"));
        Files.createDirectories(tempBase.resolve("sticker"));
        Files.write(tempBase.resolve("image/test.png"), new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        reg.add("sef-images.base-path", () -> tempBase.toString() + "/");
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void servesExistingImageWithNosniffHeader() throws Exception {
        mvc.perform(get("/image/test.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void returns404ForMissingImage() throws Exception {
        mvc.perform(get("/image/missing.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsPathTraversalReturns4xx() throws Exception {
        mvc.perform(get("/image/../../etc/passwd"))
                .andExpect(status().is4xxClientError());
    }
}
