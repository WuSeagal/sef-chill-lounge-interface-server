package com.sef.cli.announcement.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 主持人公告：以記憶體保存「目前公告」純文字（null 表示無公告）。
 * 不持久化至 DB——伺服器重啟即清空。
 */
@Service
public class AnnouncementService {

    private final AtomicReference<String> current = new AtomicReference<>();

    /** 設定目前公告；傳 null 表示清除。 */
    public void set(String text) {
        current.set(text);
    }

    /** 目前公告，無則回 null。 */
    public String getCurrent() {
        return current.get();
    }
}
