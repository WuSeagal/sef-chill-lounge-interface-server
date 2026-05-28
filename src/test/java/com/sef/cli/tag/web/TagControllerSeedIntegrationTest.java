package com.sef.cli.tag.web;

import com.sef.cli.testutil.WithMockAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the live seed in data-h2.sql aligns with the holders threshold rule.
 * Without this, a future seed edit (e.g. removing a CUS001 holder) could drift below
 * the threshold without any unit test catching it, because the existing TagControllerTest
 * uses test-local fixtures rather than the seed.
 */
@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
class TagControllerSeedIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    @WithMockAdmin(providerUserId = "u-seed-test-1")
    void seedHasHighHolderCustomVisible_butLowHolderCustomHidden() throws Exception {
        // CUS001 (露營) has 5 holders → meets threshold → should appear in CUSTOM bucket
        // CUS002 (私房菜) has 1 holder (creator only) → below threshold → hidden
        mvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.CUSTOM[*].content", hasItem("露營")))
                .andExpect(jsonPath("$.data.CUSTOM[*].content", not(hasItem("私房菜"))));
    }
}
