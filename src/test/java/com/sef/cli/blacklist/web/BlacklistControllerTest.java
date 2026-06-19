package com.sef.cli.blacklist.web;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.common.HostAuthz;
import com.sef.cli.testutil.WithMockAdmin;
import com.sef.cli.user.entity.AdminUserEntity;
import com.sef.cli.user.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
@Transactional
class BlacklistControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AdminUserRepository adminUserRepository;

    @Autowired
    AttendeeDataRepository attendeeDataRepository;

    private void seedAdminUser(String providerUserId, boolean banned) {
        adminUserRepository.save(AdminUserEntity.builder()
                .providerUserId(providerUserId)
                .email(providerUserId + "@example.com")
                .googleName("G-" + providerUserId)
                .roleName("ROLE_USER").enabled(true).firstLogin(false)
                .banned(banned).build());
    }

    private void seedAttendee(String userId, String furName, String username) {
        attendeeDataRepository.save(AttendeeDataEntity.builder()
                .userId(userId).furName(furName).username(username).build());
    }

    // ---- GET /blacklist ----

    @Test
    @WithMockAdmin(providerUserId = HostAuthz.HOST_PROVIDER_USER_ID)
    void getBlacklist_host_returns200WithEntries() throws Exception {
        seedAdminUser("bl-banned-1", true);
        seedAttendee("bl-banned-1", "Foxy", "foxxy");

        mvc.perform(get("/blacklist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[?(@.userId=='bl-banned-1')].furName").value("Foxy"))
                .andExpect(jsonPath("$.data[?(@.userId=='bl-banned-1')].username").value("foxxy"));
    }

    @Test
    @WithMockAdmin(providerUserId = "not-the-host")
    void getBlacklist_nonHost_forbidden() throws Exception {
        mvc.perform(get("/blacklist"))
                .andExpect(status().isForbidden());
    }

    // ---- POST /blacklist ----

    @Test
    @WithMockAdmin(providerUserId = HostAuthz.HOST_PROVIDER_USER_ID)
    void postBlacklist_host_bansExistingUser() throws Exception {
        seedAdminUser("bl-target-1", false);

        mvc.perform(post("/blacklist").contentType(APPLICATION_JSON)
                        .content("{\"userId\":\"bl-target-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(adminUserRepository.findByProviderUserId("bl-target-1").orElseThrow().getBanned()).isTrue();
    }

    @Test
    @WithMockAdmin(providerUserId = HostAuthz.HOST_PROVIDER_USER_ID)
    void postBlacklist_unknownUserId_failsWithoutChange() throws Exception {
        mvc.perform(post("/blacklist").contentType(APPLICATION_JSON)
                        .content("{\"userId\":\"bl-ghost\"}"))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));

        assertThat(adminUserRepository.findByProviderUserId("bl-ghost")).isEmpty();
    }

    @Test
    @WithMockAdmin(providerUserId = HostAuthz.HOST_PROVIDER_USER_ID)
    void postBlacklist_selfBanHost_rejected_andHostNotBanned() throws Exception {
        // 究極保護：host 嘗試封禁自己 → 非成功回應，且 host 仍未被封禁（host 由 data-h2.sql seed，banned=false）
        mvc.perform(post("/blacklist").contentType(APPLICATION_JSON)
                        .content("{\"userId\":\"" + HostAuthz.HOST_PROVIDER_USER_ID + "\"}"))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));

        assertThat(adminUserRepository.findByProviderUserId(HostAuthz.HOST_PROVIDER_USER_ID)
                .orElseThrow().getBanned()).isFalse();
    }

    @Test
    @WithMockAdmin(providerUserId = "not-the-host")
    void postBlacklist_nonHost_forbidden() throws Exception {
        seedAdminUser("bl-target-2", false);

        mvc.perform(post("/blacklist").contentType(APPLICATION_JSON)
                        .content("{\"userId\":\"bl-target-2\"}"))
                .andExpect(status().isForbidden());

        assertThat(adminUserRepository.findByProviderUserId("bl-target-2").orElseThrow().getBanned()).isFalse();
    }

    // ---- POST /blacklist/remove ----

    @Test
    @WithMockAdmin(providerUserId = HostAuthz.HOST_PROVIDER_USER_ID)
    void postBlacklistRemove_host_unbansUser() throws Exception {
        seedAdminUser("bl-target-3", true);

        mvc.perform(post("/blacklist/remove").contentType(APPLICATION_JSON)
                        .content("{\"userId\":\"bl-target-3\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(adminUserRepository.findByProviderUserId("bl-target-3").orElseThrow().getBanned()).isFalse();
    }

    @Test
    @WithMockAdmin(providerUserId = "not-the-host")
    void postBlacklistRemove_nonHost_forbidden() throws Exception {
        seedAdminUser("bl-target-4", true);

        mvc.perform(post("/blacklist/remove").contentType(APPLICATION_JSON)
                        .content("{\"userId\":\"bl-target-4\"}"))
                .andExpect(status().isForbidden());

        assertThat(adminUserRepository.findByProviderUserId("bl-target-4").orElseThrow().getBanned()).isTrue();
    }
}
