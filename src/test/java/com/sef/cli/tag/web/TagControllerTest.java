package com.sef.cli.tag.web;

import com.sef.cli.attendee.entity.AttendeeTagEntity;
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

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
@Transactional
class TagControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    AttendeeTagRepository attendeeTagRepository;

    @Test
    @WithMockAdmin(providerUserId = "u-tg-list-1")
    void getTags_returnsGroupedMapWithAllTypes() throws Exception {
        mvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ROLE").isArray())
                .andExpect(jsonPath("$.data.LANGUAGE").isArray())
                .andExpect(jsonPath("$.data.FRAMEWORK").isArray())
                .andExpect(jsonPath("$.data.DATABASE").isArray())
                .andExpect(jsonPath("$.data.DEVOPS").isArray())
                .andExpect(jsonPath("$.data.CUSTOM").isArray())
                // 確認每個 type bucket 內的 tag 確實是該 type — 防 grouping bug
                .andExpect(jsonPath("$.data.ROLE[*].type", everyItem(is("ROLE"))))
                .andExpect(jsonPath("$.data.LANGUAGE[*].type", everyItem(is("LANGUAGE"))))
                .andExpect(jsonPath("$.data.CUSTOM[*].type", everyItem(is("CUSTOM"))));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-tg-list-2")
    void getTags_excludesLowHolderCustom() throws Exception {
        TagEntity low = tagRepository.save(TagEntity.builder()
                .type("CUSTOM").content("only-creator-test").isCustom(true).build());
        attendeeTagRepository.save(AttendeeTagEntity.builder()
                .userId("creator-x").tagId(low.getTagId()).build());

        mvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.CUSTOM[*].content", not(hasItem("only-creator-test"))));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-tg-list-3")
    void getTags_includesHighHolderCustom() throws Exception {
        TagEntity high = tagRepository.save(TagEntity.builder()
                .type("CUSTOM").content("popular-test").isCustom(true).build());
        for (int i = 1; i <= 5; i++) {
            attendeeTagRepository.save(AttendeeTagEntity.builder()
                    .userId("u-pop-" + i).tagId(high.getTagId()).build());
        }

        mvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.CUSTOM[*].content", hasItem("popular-test")));
    }

    @Test
    void getTags_returns401_whenUnauth() throws Exception {
        mvc.perform(get("/tags"))
                .andExpect(status().isUnauthorized());
    }
}
