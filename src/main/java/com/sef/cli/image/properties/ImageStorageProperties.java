package com.sef.cli.image.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "sef-images")
public class ImageStorageProperties {

    private String basePath;
    private int maxFileSizeMb;
    private Chat chat = new Chat();
    private User user = new User();
    private Sticker sticker = new Sticker();

    @Getter
    @Setter
    public static class Chat {
        private String urlPrefix;
        private int maxCount;
        private int ttlDays;
    }

    @Getter
    @Setter
    public static class User {
        private String urlPrefix;
    }

    @Getter
    @Setter
    public static class Sticker {
        private String urlPrefix;
    }
}
