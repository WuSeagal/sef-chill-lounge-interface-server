package com.sef.cli.attendee.web;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.entity.AttendeeTagEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.attendee.repository.AttendeeTagRepository;
import com.sef.cli.tag.entity.TagEntity;
import com.sef.cli.tag.repository.TagRepository;
import com.sef.cli.testutil.WithMockAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
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

    @Autowired
    TagRepository tagRepository;

    @Autowired
    AttendeeTagRepository attendeeTagRepository;

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

    @Test
    @WithMockAdmin(providerUserId = "u-m-tags")
    void getMembers_includesTags() throws Exception {
        attendeeDataRepository.save(AttendeeDataEntity.builder()
                .userId("u-m-T").username("T").furName("Tfur").build());
        tagRepository.save(TagEntity.builder()
                .tagId("tag-be").type("ROLE").content("後端").isCustom(false).build());
        tagRepository.save(TagEntity.builder()
                .tagId("tag-rust").type("LANGUAGE").content("Rust").isCustom(false).build());
        attendeeTagRepository.save(AttendeeTagEntity.builder().userId("u-m-T").tagId("tag-be").build());
        attendeeTagRepository.save(AttendeeTagEntity.builder().userId("u-m-T").tagId("tag-rust").build());

        mvc.perform(get("/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.userId=='u-m-T')].tags[*].content", hasItem("後端")))
                .andExpect(jsonPath("$.data[?(@.userId=='u-m-T')].tags[*].content", hasItem("Rust")));
    }
}
