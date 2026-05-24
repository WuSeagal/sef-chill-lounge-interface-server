package com.sef.cli.tag.web;

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

    @Test
    @WithMockAdmin(providerUserId = "u-tg-list-1")
    void getTags_returnsNonCustomOnly() throws Exception {
        // seed 內有 species / hobby / custom；GET /tags 應只回非 custom
        mvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].type", everyItem(not(is("custom")))));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-tg-list-2")
    void getTags_includesCustomCreatedAfterIsExcluded() throws Exception {
        TagEntity custom = TagEntity.builder().type("custom").content("test-custom").build();
        tagRepository.save(custom);

        mvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].type", everyItem(not(is("custom")))));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-tg-list-3")
    void getTags_emptyArray_whenAllCustomOrNone() throws Exception {
        tagRepository.deleteAll();

        mvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getTags_returns401_whenUnauth() throws Exception {
        mvc.perform(get("/tags"))
                .andExpect(status().isUnauthorized());
    }
}
