package com.sef.cli.image.properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ImageStoragePropertiesTest.TestConfig.class)
@TestPropertySource(properties = {
        "sef-images.base-path=/tmp/test-images/",
        "sef-images.max-file-size-mb=10",
        "sef-images.chat.url-prefix=/image/",
        "sef-images.chat.max-count=1000",
        "sef-images.chat.ttl-days=90",
        "sef-images.user.url-prefix=/user/",
        "sef-images.sticker.url-prefix=/sticker/"
})
class ImageStoragePropertiesTest {

    @Configuration
    @EnableConfigurationProperties(ImageStorageProperties.class)
    static class TestConfig {
    }

    @Autowired
    private ImageStorageProperties properties;

    @Test
    void bindsAllFieldsFromYaml() {
        assertThat(properties.getBasePath()).isEqualTo("/tmp/test-images/");
        assertThat(properties.getMaxFileSizeMb()).isEqualTo(10);
        assertThat(properties.getChat().getUrlPrefix()).isEqualTo("/image/");
        assertThat(properties.getChat().getMaxCount()).isEqualTo(1000);
        assertThat(properties.getChat().getTtlDays()).isEqualTo(90);
        assertThat(properties.getUser().getUrlPrefix()).isEqualTo("/user/");
        assertThat(properties.getSticker().getUrlPrefix()).isEqualTo("/sticker/");
    }
}
