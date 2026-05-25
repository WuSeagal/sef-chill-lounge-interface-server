package com.sef.cli.chat.event.response;

import java.util.List;

public record PresenceSnapshotPayload(List<String> onlineUserIds) {
}
