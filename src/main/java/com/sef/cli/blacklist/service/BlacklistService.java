package com.sef.cli.blacklist.service;

import com.sef.cli.api.response.BlacklistEntryResponse;
import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.attendee.web.map.BlacklistDtoMapper;
import com.sef.cli.common.HostAuthz;
import com.sef.cli.user.entity.AdminUserEntity;
import com.sef.cli.user.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * host 黑名單維護：以 ADMIN_USER.banned 為唯一真相來源（D1），顯示名 join ATTENDEE_DATA。
 * 封禁鍵為 userId（= providerUserId，D5）。
 */
@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final AdminUserRepository adminUserRepository;
    private final AttendeeDataRepository attendeeDataRepository;
    private final BlacklistDtoMapper blacklistDtoMapper;

    /** 目前被封禁清單：banned 的 AdminUser join AttendeeData 顯示名（無 profile 者會被 join 排除）。 */
    public List<BlacklistEntryResponse> list() {
        List<String> bannedUserIds = adminUserRepository.findByBannedTrue().stream()
                .map(AdminUserEntity::getProviderUserId)
                .toList();
        if (bannedUserIds.isEmpty()) {
            return List.of();
        }
        List<AttendeeDataEntity> attendees = attendeeDataRepository.findByUserIdIn(bannedUserIds);
        return blacklistDtoMapper.toEntryList(attendees);
    }

    /** 封禁：存在則設 banned=true 並回 true；userId 不存在回 false 且不改任何資料。 */
    public boolean ban(String userId) {
        // 究極保護：host 不可被封禁（即使 target=host）。網域規則，防守縱深，回 false。
        if (HostAuthz.isHost(userId)) {
            return false;
        }
        return adminUserRepository.findByProviderUserId(userId)
                .map(user -> {
                    user.setBanned(true);
                    adminUserRepository.save(user);
                    return true;
                })
                .orElse(false);
    }

    /** 解封：存在則設 banned=false；不存在不動作。 */
    public void unban(String userId) {
        adminUserRepository.findByProviderUserId(userId).ifPresent(user -> {
            user.setBanned(false);
            adminUserRepository.save(user);
        });
    }
}
