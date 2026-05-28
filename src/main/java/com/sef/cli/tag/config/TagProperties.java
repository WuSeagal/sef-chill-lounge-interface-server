package com.sef.cli.tag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.tag")
@Getter
@Setter
public class TagProperties {
    private int maxPerUser = 20;
    private int customHoldersThreshold = 5;
}
