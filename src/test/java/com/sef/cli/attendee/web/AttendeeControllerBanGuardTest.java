package com.sef.cli.attendee.web;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.testutil.WithMockAdmin;
import com.sef.cli.user.entity.AdminUserEntity;
import com.sef.cli.user.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BanGuard 對異動類 REST 守門（design.md D3 / host-user-ban spec）：
 * banned 使用者呼叫異動 endpoint 回 403；讀取類與 check-auth 不被擋。
 */
@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
@Transactional
class AttendeeControllerBanGuardTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AttendeeDataRepository attendeeDataRepository;

    @Autowired
    AdminUserRepository adminUserRepository;

    @MockitoBean
    ChatBroadcastService chatBroadcastService;

    private void seedBannedAdmin(String providerUserId) {
        adminUserRepository.save(AdminUserEntity.builder()
                .providerUserId(providerUserId)
                .email(providerUserId + "@example.com")
                .googleName("G")
                .roleName("ROLE_USER").enabled(true).firstLogin(false)
                .banned(true).build());
    }

    private void seedProfile(String userId) {
        attendeeDataRepository.save(AttendeeDataEntity.builder()
                .userId(userId).username("Foo").furName("FooFur").avatarColor("#FF0000").build());
    }

    @Test
    @WithMockAdmin(providerUserId = "ban-mut-1")
    void bannedUser_updateProfile_returns403() throws Exception {
        seedBannedAdmin("ban-mut-1");
        seedProfile("ban-mut-1");

        mvc.perform(post("/user/profile/update").contentType(APPLICATION_JSON)
                        .content("{\"furName\":\"NEW\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAdmin(providerUserId = "ban-mut-2")
    void bannedUser_createProfile_returns403() throws Exception {
        seedBannedAdmin("ban-mut-2");

        mvc.perform(post("/user/profile").contentType(APPLICATION_JSON)
                        .content("{\"furName\":\"X\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAdmin(providerUserId = "ban-mut-3")
    void bannedUser_redrawTopicCard_returns403() throws Exception {
        seedBannedAdmin("ban-mut-3");
        seedProfile("ban-mut-3");

        mvc.perform(post("/user/topic-card/redraw"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAdmin(providerUserId = "ban-mut-4")
    void bannedUser_getOwnProfile_notBlocked() throws Exception {
        seedBannedAdmin("ban-mut-4");
        seedProfile("ban-mut-4");

        // 讀取類不被 ban 守門擋下（profile 存在 → 200）
        mvc.perform(get("/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("ban-mut-4"));
    }

    @Test
    @WithMockAdmin(providerUserId = "ban-mut-5")
    void bannedUser_checkAuth_notBlocked_andBannedTrue() throws Exception {
        seedBannedAdmin("ban-mut-5");

        mvc.perform(get("/check-auth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.banned").value(true));
    }

    @Test
    @WithMockAdmin(providerUserId = "notbanned-mut-1")
    void notBannedUser_updateProfile_notBlocked() throws Exception {
        // 未在 ADMIN_USER 建立 banned 列 → BanGuard.isBanned=false → 正常更新
        seedProfile("notbanned-mut-1");

        mvc.perform(post("/user/profile/update").contentType(APPLICATION_JSON)
                        .content("{\"furName\":\"NEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.furName").value("NEW"));
    }
}
