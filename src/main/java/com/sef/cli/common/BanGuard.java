package com.sef.cli.common;

import com.sef.cli.common.exception.ForbiddenException;
import com.sef.cli.user.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 即時封禁判定。一律以 providerUserId 即時查 {@link AdminUserRepository} 的當下 banned 狀態，
 * 不讀取登入當下序列化於 session 的 principal 快照（design.md D2）。供 WS 連線守門與
 * 異動類 REST 守門共用，避免邏輯散落（design.md D3）。
 */
@Component
@RequiredArgsConstructor
public class BanGuard {

    private final AdminUserRepository adminUserRepository;

    /** 即時查 DB 判斷該 providerUserId 是否被封禁；查無使用者或 providerUserId 為 null 皆視為未封禁。 */
    public boolean isBanned(String providerUserId) {
        if (providerUserId == null) {
            return false;
        }
        return adminUserRepository.findByProviderUserId(providerUserId)
                .map(user -> Boolean.TRUE.equals(user.getBanned()))
                .orElse(false);
    }

    /** 被封禁則拋 {@link ForbiddenException}（對應 403）；未封禁正常返回。 */
    public void assertNotBanned(String providerUserId) {
        if (isBanned(providerUserId)) {
            throw new ForbiddenException();
        }
    }
}
