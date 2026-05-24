package com.sef.cli.topic.web;

import com.sef.cli.testutil.WithMockAdmin;
import com.sef.cli.topic.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
@Transactional
class TopicControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TopicRepository topicRepository;

    @Test
    @WithMockAdmin(providerUserId = "u-topic-1")
    void getRandom_returns200_whenPoolNotEmpty() throws Exception {
        // data-h2.sql 已 seed 5 topics
        mvc.perform(get("/topics/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.topicId").exists())
                .andExpect(jsonPath("$.data.content").exists());
    }

    @Test
    @WithMockAdmin(providerUserId = "u-topic-2")
    void getRandom_returns404_whenPoolEmpty() throws Exception {
        topicRepository.deleteAll();

        mvc.perform(get("/topics/random"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("no_topic_available"));
    }

    @Test
    void getRandom_returns401_whenNotAuthenticated() throws Exception {
        mvc.perform(get("/topics/random"))
                .andExpect(status().isUnauthorized());
    }
}
