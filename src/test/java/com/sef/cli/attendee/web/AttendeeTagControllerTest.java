package com.sef.cli.attendee.web;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
@Transactional
class AttendeeTagControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AttendeeTagRepository attendeeTagRepository;

    @Autowired
    TagRepository tagRepository;

    @Test
    @WithMockAdmin(providerUserId = "u-tg-1")
    void postTags_existing_returns201() throws Exception {
        // 用 seed 既存 default tag 之一
        TagEntity anyTag = tagRepository.findAll().get(0);

        mvc.perform(post("/user/tags").contentType(APPLICATION_JSON)
                        .content("{\"tagId\":\"" + anyTag.getTagId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tagId").value(anyTag.getTagId()));
        assertThat(attendeeTagRepository.existsByUserIdAndTagId("u-tg-1", anyTag.getTagId())).isTrue();
    }

    @Test
    @WithMockAdmin(providerUserId = "u-tg-2")
    void postTags_custom_creates() throws Exception {
        mvc.perform(post("/user/tags").contentType(APPLICATION_JSON)
                        .content("{\"type\":\"custom\",\"content\":\"我的新tag\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("我的新tag"))
                .andExpect(jsonPath("$.data.tagId").exists());
    }

    @Test
    @WithMockAdmin(providerUserId = "u-tg-3")
    void postTags_dup_returns409() throws Exception {
        TagEntity anyTag = tagRepository.findAll().get(0);
        attendeeTagRepository.save(AttendeeTagEntity.builder()
                .userId("u-tg-3").tagId(anyTag.getTagId()).build());

        mvc.perform(post("/user/tags").contentType(APPLICATION_JSON)
                        .content("{\"tagId\":\"" + anyTag.getTagId() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("tag_already_associated"));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-tg-4")
    void postRemoveTag_removes() throws Exception {
        TagEntity anyTag = tagRepository.findAll().get(0);
        attendeeTagRepository.save(AttendeeTagEntity.builder()
                .userId("u-tg-4").tagId(anyTag.getTagId()).build());

        mvc.perform(post("/user/tags/remove").contentType(APPLICATION_JSON)
                        .content("{\"tagId\":\"" + anyTag.getTagId() + "\"}"))
                .andExpect(status().isOk());
        assertThat(attendeeTagRepository.existsByUserIdAndTagId("u-tg-4", anyTag.getTagId())).isFalse();
    }

    @Test
    @WithMockAdmin(providerUserId = "u-tg-5")
    void postRemoveTag_missing_returns404() throws Exception {
        mvc.perform(post("/user/tags/remove").contentType(APPLICATION_JSON)
                        .content("{\"tagId\":\"never-existed\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("tag_junction_not_found"));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-tg-6")
    void postTags_returns400_whenLimitExceeded() throws Exception {
        for (int i = 1; i <= 20; i++) {
            attendeeTagRepository.save(AttendeeTagEntity.builder()
                    .userId("u-tg-6").tagId("filler-" + i).build());
        }
        TagEntity anyTag = tagRepository.findAll().get(0);

        mvc.perform(post("/user/tags").contentType(APPLICATION_JSON)
                        .content("{\"tagId\":\"" + anyTag.getTagId() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("tag_limit_exceeded"));
    }
}
