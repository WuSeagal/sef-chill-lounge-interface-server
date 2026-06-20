package com.sef.cli.chat.service;

import com.sef.cli.chat.event.ChatEnvelope;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.event.response.PresenceSnapshotPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 排程回收 idle/幽靈 presence（D6）。每 30 秒呼叫 {@link OnlineUserService#reapStale(long)}
 * 移除已關閉或閒置逾 {@value #IDLE_TIMEOUT_MS}ms（90s，約連續漏 3 次心跳）的 session；
 * 有移除時重廣播 PRESENCE_SNAPSHOT，使在線數與 @mention 名單貼近現場。
 *
 * <p>獨立於 {@link OnlineUserService}，避免該服務反向依賴 {@link ChatBroadcastService}
 * （後者已依賴 OnlineUserService，會形成循環）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceReaperService {

    static final long IDLE_TIMEOUT_MS = 90_000L;

    private final OnlineUserService onlineUserService;
    private final ChatBroadcastService broadcastService;

    @Scheduled(fixedDelay = 30_000L)
    public void reap() {
        List<String> removed = onlineUserService.reapStale(IDLE_TIMEOUT_MS);
        if (removed.isEmpty()) {
            return;
        }
        log.info("[PRESENCE_REAP] 回收 idle/幽靈 session, removed={}, online={}",
                removed, onlineUserService.getOnlineUserIds().size());
        PresenceSnapshotPayload snapshot = new PresenceSnapshotPayload(new ArrayList<>(onlineUserService.getOnlineUserIds()));
        broadcastService.broadcastToAll(new ChatEnvelope<>(ChatEventType.PRESENCE_SNAPSHOT, System.currentTimeMillis(), snapshot));
    }
}
