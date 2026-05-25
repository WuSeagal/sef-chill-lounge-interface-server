package com.sef.cli.message.web;

import com.sef.cli.message.enums.MessageType;
import com.sef.cli.message.service.MessageService;
import com.sef.cli.message.service.dto.MessageHistoryData;
import com.sef.cli.testutil.WithMockAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                        "hello",
                        List.of(),
                        null,
                        LocalDateTime.of(2026, 5, 25, 10, 0, 0)
                )
        ));

        mvc.perform(get("/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].cursorId").value(11))
                .andExpect(jsonPath("$.data[0].messageId").value("msg-001"))
                .andExpect(jsonPath("$.data[0].messageType").value("TEXT"))
                .andExpect(jsonPath("$.data[0].furName").value("Fox"));
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
}
