package com.sef.cli.attendee.web;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.testutil.WithMockAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AttendeeDataRepository attendeeDataRepository;

    @Test
    @WithMockAdmin(providerUserId = "u-m-1")
    void getMembers_returnsAll() throws Exception {
        attendeeDataRepository.save(AttendeeDataEntity.builder()
                .userId("u-m-A").username("A").furName("Afur").build());
        attendeeDataRepository.save(AttendeeDataEntity.builder()
                .userId("u-m-B").username("B").furName("Bfur").build());

        mvc.perform(get("/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-m-2")
    void getMembers_emptyArray_whenNone() throws Exception {
        attendeeDataRepository.deleteAll();

        mvc.perform(get("/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getMembers_returns401_whenUnauth() throws Exception {
        mvc.perform(get("/members"))
                .andExpect(status().isUnauthorized());
    }
}
