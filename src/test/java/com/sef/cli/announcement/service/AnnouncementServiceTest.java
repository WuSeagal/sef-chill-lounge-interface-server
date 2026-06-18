package com.sef.cli.announcement.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnnouncementServiceTest {

    private final AnnouncementService service = new AnnouncementService();

    @Test
    void initiallyNull() {
        assertThat(service.getCurrent()).isNull();
    }

    @Test
    void setThenGet() {
        service.set("活動 18:00 開始");
        assertThat(service.getCurrent()).isEqualTo("活動 18:00 開始");
    }

    @Test
    void setNullClears() {
        service.set("x");
        service.set(null);
        assertThat(service.getCurrent()).isNull();
    }
}
