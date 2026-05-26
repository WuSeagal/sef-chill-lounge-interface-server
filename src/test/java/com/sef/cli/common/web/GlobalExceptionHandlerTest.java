package com.sef.cli.common.web;

import com.sef.cli.common.exception.ForbiddenException;
import com.sef.cli.common.exception.InvalidTopicIdException;
import com.sef.cli.common.exception.NoOtherTopicAvailableException;
import com.sef.cli.common.exception.NoTopicAvailableException;
import com.sef.cli.common.exception.ProfileAlreadyExistsException;
import com.sef.cli.common.exception.ProfileNotFoundException;
import com.sef.cli.common.exception.SocialLinkNotFoundException;
import com.sef.cli.common.exception.TagAlreadyAssociatedException;
import com.sef.cli.common.exception.TagJunctionNotFoundException;
import com.sef.cli.image.web.exception.PayloadTooLargeException;
import com.sef.cli.image.web.exception.UnsupportedMediaTypeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerTest.StubController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @RestController
    static class StubController {
        @GetMapping("/__test__/profile-not-found")
        public void profileNotFound() {
            throw new ProfileNotFoundException();
        }

        @GetMapping("/__test__/already-exists")
        public void alreadyExists() {
            throw new ProfileAlreadyExistsException();
        }

        @GetMapping("/__test__/tag-dup")
        public void tagDup() {
            throw new TagAlreadyAssociatedException();
        }

        @GetMapping("/__test__/tag-junction-not-found")
        public void tagJunctionNotFound() {
            throw new TagJunctionNotFoundException();
        }

        @GetMapping("/__test__/social-link-not-found")
        public void socialLinkNotFound() {
            throw new SocialLinkNotFoundException();
        }

        @GetMapping("/__test__/no-topic")
        public void noTopic() {
            throw new NoTopicAvailableException();
        }

        @GetMapping("/__test__/no-other-topic")
        public void noOtherTopic() {
            throw new NoOtherTopicAvailableException();
        }

        @GetMapping("/__test__/invalid-topic")
        public void invalidTopic() {
            throw new InvalidTopicIdException();
        }

        @GetMapping("/__test__/forbidden")
        public void forbidden() {
            throw new ForbiddenException();
        }

        @GetMapping("/__test__/illegal-argument")
        public void illegalArgument() {
            throw new IllegalArgumentException("custom_message");
        }

        @GetMapping("/__test__/data-integrity")
        public void dataIntegrity() {
            throw new DataIntegrityViolationException("UNIQUE constraint violated");
        }

        @GetMapping("/__test__/payload-too-large")
        public void payloadTooLarge() {
            throw new PayloadTooLargeException("file_too_large", 10);
        }

        @GetMapping("/__test__/unsupported-media")
        public void unsupportedMedia() {
            throw new UnsupportedMediaTypeException("unsupported_image_type");
        }

        @GetMapping("/__test__/boom")
        public void boom() {
            throw new RuntimeException("boom for test");
        }
    }

    @Test
    void mapsProfileNotFound_to404() throws Exception {
        mvc.perform(get("/__test__/profile-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("profile_not_found"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void mapsProfileAlreadyExists_to409() throws Exception {
        mvc.perform(get("/__test__/already-exists"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("profile_already_exists"));
    }

    @Test
    void mapsTagAlreadyAssociated_to409() throws Exception {
        mvc.perform(get("/__test__/tag-dup"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("tag_already_associated"));
    }

    @Test
    void mapsTagJunctionNotFound_to404() throws Exception {
        mvc.perform(get("/__test__/tag-junction-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("tag_junction_not_found"));
    }

    @Test
    void mapsSocialLinkNotFound_to404() throws Exception {
        mvc.perform(get("/__test__/social-link-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("social_link_not_found"));
    }

    @Test
    void mapsNoTopicAvailable_to404() throws Exception {
        mvc.perform(get("/__test__/no-topic"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("no_topic_available"));
    }

    @Test
    void mapsNoOtherTopic_to409() throws Exception {
        mvc.perform(get("/__test__/no-other-topic"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("no_other_topic_available"));
    }

    @Test
    void mapsInvalidTopicId_to400() throws Exception {
        mvc.perform(get("/__test__/invalid-topic"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid_topic_id"));
    }

    @Test
    void mapsForbidden_to403() throws Exception {
        mvc.perform(get("/__test__/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("forbidden"));
    }

    @Test
    void mapsIllegalArgument_to400_withMessageEcho() throws Exception {
        mvc.perform(get("/__test__/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("custom_message"));
    }

    @Test
    void mapsDataIntegrityViolation_to409_constraintViolation() throws Exception {
        mvc.perform(get("/__test__/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("constraint_violation"));
    }

    @Test
    void mapsPayloadTooLarge_to413_withMaxSizeData() throws Exception {
        mvc.perform(get("/__test__/payload-too-large"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value(413))
                .andExpect(jsonPath("$.message").value("file_too_large"))
                .andExpect(jsonPath("$.data.maxSizeMB").value(10));
    }

    @Test
    void mapsUnsupportedMediaType_to415() throws Exception {
        mvc.perform(get("/__test__/unsupported-media"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(415))
                .andExpect(jsonPath("$.message").value("unsupported_image_type"));
    }

    @Test
    void mapsUnhandledRuntime_to500_withTraceId() throws Exception {
        mvc.perform(get("/__test__/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系統暫時無法處理您的請求，請稍後再試"))
                .andExpect(jsonPath("$.traceId").value(org.hamcrest.Matchers.matchesPattern("^[a-f0-9]{8}$")));
    }

    @Test
    void mapsNoResourceFound_to404_withGenericMessage() throws Exception {
        mvc.perform(get("/api/no-such-endpoint-xyz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("找不到資源"))
                .andExpect(jsonPath("$.traceId").value(org.hamcrest.Matchers.matchesPattern("^[a-f0-9]{8}$")));
    }
}
