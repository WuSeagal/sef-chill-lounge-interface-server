package com.sef.cli.message.web;

import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.event.response.MessageDeletedPayload;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.common.HostAuthz;
import com.sef.cli.common.exception.ForbiddenException;
import com.sef.cli.common.exception.MessageNotFoundException;
import com.sef.cli.message.enums.MessageType;
import com.sef.cli.message.service.MessageService;
import com.sef.cli.message.service.dto.MessageHistoryData;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
@Transactional
class MessageControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MessageService messageService;

    @MockitoBean
    ChatBroadcastService chatBroadcastService;

    @Test
    @WithMockAdmin(providerUserId = "msg-user-001")
    void returnsLatestMessages() throws Exception {
        when(messageService.loadHistory(null, null, 50)).thenReturn(List.of(
                new MessageHistoryData(
                        11L,
                        "msg-001",
                        "google-001",
                        MessageType.TEXT,
                        "Fox",
                        "/avatar.png",
                        "#7b9b8f",
                        true,
                        "hello",
                        List.of(),
                        null,
                        LocalDateTime.of(2026, 5, 25, 10, 0, 0),
                        null,
                        null,
                        null,
                        null,
                        null
                )
        ));

        mvc.perform(get("/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].cursorId").value(11))
                .andExpect(jsonPath("$.data[0].messageId").value("msg-001"))
                .andExpect(jsonPath("$.data[0].messageType").value("TEXT"))
                .andExpect(jsonPath("$.data[0].furName").value("Fox"))
                .andExpect(jsonPath("$.data[0].avatarColor").value("#7b9b8f"))
                .andExpect(jsonPath("$.data[0].avatarBorder").value(true));
    }

    @Test
    @WithMockAdmin(providerUserId = "msg-user-002")
    void passesCursorParamsToService() throws Exception {
        when(messageService.loadHistory(
                LocalDateTime.of(2026, 5, 25, 10, 0, 0),
                11L,
                50
        )).thenReturn(List.of());

        mvc.perform(get("/messages")
                        .param("before", "2026-05-25T10:00:00")
                        .param("beforeId", "11")
                        .param("limit", "50"))
                .andExpect(status().isOk());

        verify(messageService).loadHistory(LocalDateTime.of(2026, 5, 25, 10, 0, 0), 11L, 50);
    }

    @Test
    @WithMockAdmin(providerUserId = "msg-user-003")
    void passesRawLimitToServiceForCapping() throws Exception {
        when(messageService.loadHistory(null, null, 999)).thenReturn(List.of());

        mvc.perform(get("/messages").param("limit", "999"))
                .andExpect(status().isOk());

        verify(messageService).loadHistory(null, null, 999);
    }

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mvc.perform(get("/messages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("unauthenticated"));
    }

    @Test
    @WithMockAdmin(providerUserId = HostAuthz.HOST_PROVIDER_USER_ID)
    void hostDeleteReturns200EnvelopeAndBroadcastsMessageDeleted() throws Exception {
        when(messageService.softDelete("m-1", HostAuthz.HOST_PROVIDER_USER_ID)).thenReturn(true);

        try (LogCaptor captor = LogCaptor.forClass(MessageController.class)) {
            mvc.perform(post("/messages/remove").contentType(APPLICATION_JSON).content("{\"messageId\":\"m-1\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            captor.assertLogged(Level.INFO, "[MESSAGE_DELETE]", "messageId=m-1");
        }

        verify(chatBroadcastService).broadcastToAll(argThat(env ->
                env.type() == ChatEventType.MESSAGE_DELETED
                        && env.data() instanceof MessageDeletedPayload payload
                        && payload.messageId().equals("m-1")));
    }

    @Test
    @WithMockAdmin(providerUserId = "not-the-host")
    void nonHostDeleteReturns403AndDoesNotBroadcast() throws Exception {
        when(messageService.softDelete("m-1", "not-the-host")).thenThrow(new ForbiddenException());

        mvc.perform(post("/messages/remove").contentType(APPLICATION_JSON).content("{\"messageId\":\"m-1\"}"))
                .andExpect(status().isForbidden());

        verify(chatBroadcastService, never()).broadcastToAll(any());
    }

    @Test
    @WithMockAdmin(providerUserId = HostAuthz.HOST_PROVIDER_USER_ID)
    void deleteMissingMessageReturns404() throws Exception {
        when(messageService.softDelete("nope", HostAuthz.HOST_PROVIDER_USER_ID))
                .thenThrow(new MessageNotFoundException());

        mvc.perform(post("/messages/remove").contentType(APPLICATION_JSON).content("{\"messageId\":\"nope\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockAdmin(providerUserId = HostAuthz.HOST_PROVIDER_USER_ID)
    void idempotentDeleteReturns200WithoutBroadcast() throws Exception {
        when(messageService.softDelete("m-1", HostAuthz.HOST_PROVIDER_USER_ID)).thenReturn(false);

        mvc.perform(post("/messages/remove").contentType(APPLICATION_JSON).content("{\"messageId\":\"m-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(chatBroadcastService, never()).broadcastToAll(any());
    }

    @Test
    void deleteUnauthenticatedReturns401() throws Exception {
        mvc.perform(post("/messages/remove").contentType(APPLICATION_JSON).content("{\"messageId\":\"m-1\"}"))
                .andExpect(status().isUnauthorized());
    }
}
