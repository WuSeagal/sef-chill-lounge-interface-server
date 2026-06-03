package com.sef.cli.attendee.web;

import com.sef.cli.attendee.entity.AttendeeSocialEntity;
import com.sef.cli.attendee.enums.PlatformEnum;
import com.sef.cli.attendee.repository.AttendeeSocialRepository;
import com.sef.cli.testutil.WithMockAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
@Transactional
class AttendeeSocialControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AttendeeSocialRepository repo;

    @Test
    @WithMockAdmin(providerUserId = "u-s-1")
    void postSocial_creates() throws Exception {
        mvc.perform(post("/user/social-links").contentType(APPLICATION_JSON)
                        .content("{\"platform\":\"X\",\"links\":\"https://x.com/testuser\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.platform").value("X"));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-s-2")
    void postRemoveSocial_ownEntry_returns200() throws Exception {
        AttendeeSocialEntity saved = repo.save(AttendeeSocialEntity.builder()
                .userId("u-s-2").platform(PlatformEnum.INSTAGRAM).links("https://instagram.com/test").build());

        mvc.perform(post("/user/social-links/remove").contentType(APPLICATION_JSON)
                        .content("{\"id\":" + saved.getId() + "}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockAdmin(providerUserId = "u-s-3")
    void postRemoveSocial_other_returns403() throws Exception {
        AttendeeSocialEntity saved = repo.save(AttendeeSocialEntity.builder()
                .userId("u-OTHER").platform(PlatformEnum.INSTAGRAM).links("https://instagram.com/test").build());

        mvc.perform(post("/user/social-links/remove").contentType(APPLICATION_JSON)
                        .content("{\"id\":" + saved.getId() + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("forbidden"));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-s-4")
    void postRemoveSocial_missing_returns404() throws Exception {
        mvc.perform(post("/user/social-links/remove").contentType(APPLICATION_JSON)
                        .content("{\"id\":99999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("social_link_not_found"));
    }
}
