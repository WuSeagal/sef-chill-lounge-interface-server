package com.sef.cli.common;

/**
 * 主持人（host）授權判定。host 帳號寫死為單一 Google provider user id，
 * 賦予現場內容管理權限（如刪除訊息、後續的公告功能）。後端為唯一授權邊界，
 * 前端僅以鏡像常數控制 UI 顯隱，不得作為安全依據。
 */
public final class HostAuthz {

    public static final String HOST_PROVIDER_USER_ID = "111427449810799428954";

    private HostAuthz() {
    }

    public static boolean isHost(String providerUserId) {
        return HOST_PROVIDER_USER_ID.equals(providerUserId);
    }
}
