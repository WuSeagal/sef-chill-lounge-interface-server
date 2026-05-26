package com.sef.cli.image.config;

import com.sef.cli.image.properties.ImageStorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ImageStorageProperties.class)
public class ImageWebConfig {
    // Resource handlers and nosniff interceptor added in Task 8.
}
