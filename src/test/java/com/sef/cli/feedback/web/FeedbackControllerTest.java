package com.sef.cli.feedback.web;

import com.sef.cli.api.request.FeedbackRequest;
import com.sef.cli.feedback.service.FeedbackMailService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
@Transactional
class FeedbackControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    FeedbackMailService feedbackMailService;

    @Autowired
    AdminUserRepository adminUserRepository;

    private void seedBannedAdmin(String providerUserId) {
        adminUserRepository.save(AdminUserEntity.builder()
                .providerUserId(providerUserId)
                .email(providerUserId + "@example.com").googleName("G")
                .roleName("ROLE_USER").enabled(true).firstLogin(false)
                .banned(true).build());
    }

    @Test
    @WithMockAdmin(providerUserId = "fb-banned")
    void submit_returns403AndDoesNotSend_whenBanned() throws Exception {
        seedBannedAdmin("fb-banned");

        mvc.perform(post("/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\",\"username\":\"u\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(feedbackMailService);
    }

    @Test
    @WithMockAdmin(providerUserId = "fb-user-1")
    void submit_returns200AndSends_whenValid() throws Exception {
        mvc.perform(post("/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\",\"username\":\"u\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(feedbackMailService).send(any(FeedbackRequest.class));
    }

    @Test
    @WithMockAdmin(providerUserId = "fb-user-2")
    void submit_returns400AndDoesNotSend_whenTitleBlank() throws Exception {
        mvc.perform(post("/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"c\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedbackMailService);
    }

    @Test
    @WithMockAdmin(providerUserId = "fb-user-3")
    void submit_returns400AndDoesNotSend_whenContentBlank() throws Exception {
        mvc.perform(post("/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedbackMailService);
    }

    @Test
    @WithMockAdmin(providerUserId = "fb-user-4")
    void submit_returns500_whenMailSendFails() throws Exception {
        doThrow(new RuntimeException("smtp down")).when(feedbackMailService).send(any(FeedbackRequest.class));

        mvc.perform(post("/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void submit_returns401_whenUnauthenticated() throws Exception {
        mvc.perform(post("/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isUnauthorized());
    }
}
