package com.sef.cli.announcement.web;

import com.sef.cli.announcement.service.AnnouncementService;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.event.response.AnnouncementPayload;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.common.HostAuthz;
import com.sef.cli.testutil.LogCaptor;
import com.sef.cli.testutil.WithMockAdmin;
import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
@Transactional
class AnnouncementControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AnnouncementService announcementService;

    @MockitoBean
    ChatBroadcastService chatBroadcastService;

    @Test
    @WithMockAdmin(providerUserId = HostAuthz.HOST_PROVIDER_USER_ID)
    void hostSetReturns200BroadcastsAndLogs() throws Exception {
        try (LogCaptor captor = LogCaptor.forClass(AnnouncementController.class)) {
            mvc.perform(post("/announcement").contentType(APPLICATION_JSON).content("{\"text\":\"B12 抽獎\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
            captor.assertLogged(Level.INFO, "[ANNOUNCEMENT]", "action=pin");
        }

        verify(announcementService).set("B12 抽獎");
        verify(chatBroadcastService).broadcastToAll(argThat(env ->
                env.type() == ChatEventType.ANNOUNCEMENT
                        && env.data() instanceof AnnouncementPayload p
                        && "B12 抽獎".equals(p.text())));
    }

    @Test
    @WithMockAdmin(providerUserId = HostAuthz.HOST_PROVIDER_USER_ID)
    void hostEmptyClearsAndBroadcastsNull() throws Exception {
        mvc.perform(post("/announcement").contentType(APPLICATION_JSON).content("{\"text\":\"   \"}"))
                .andExpect(status().isOk());

        verify(announcementService).set(null);
        verify(chatBroadcastService).broadcastToAll(argThat(env ->
                env.type() == ChatEventType.ANNOUNCEMENT
                        && env.data() instanceof AnnouncementPayload p
                        && p.text() == null));
    }

    @Test
    @WithMockAdmin(providerUserId = "not-the-host")
    void nonHostReturns403() throws Exception {
        mvc.perform(post("/announcement").contentType(APPLICATION_JSON).content("{\"text\":\"hi\"}"))
                .andExpect(status().isForbidden());

        verify(announcementService, never()).set(any());
        verify(chatBroadcastService, never()).broadcastToAll(any());
    }

    @Test
    @WithMockAdmin(providerUserId = HostAuthz.HOST_PROVIDER_USER_ID)
    void tooLongReturns400() throws Exception {
        String longText = "a".repeat(201);
        mvc.perform(post("/announcement").contentType(APPLICATION_JSON).content("{\"text\":\"" + longText + "\"}"))
                .andExpect(status().isBadRequest());

        verify(announcementService, never()).set(any());
    }

    @Test
    void unauthenticatedReturns401() throws Exception {
        mvc.perform(post("/announcement").contentType(APPLICATION_JSON).content("{\"text\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }
}
