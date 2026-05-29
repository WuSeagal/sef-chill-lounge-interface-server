package com.sef.cli.attendee.web;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.chat.event.ChatEnvelope;
import com.sef.cli.chat.event.ChatEventType;
import com.sef.cli.chat.event.response.ProfileUpdatedPayload;
import com.sef.cli.chat.service.ChatBroadcastService;
import com.sef.cli.testutil.WithMockAdmin;
import com.sef.cli.topic.entity.TopicEntity;
import com.sef.cli.topic.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.sef.cli.SefChillLoungeServerApplication.class)
@AutoConfigureMockMvc
@Transactional
class AttendeeControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AttendeeDataRepository attendeeDataRepository;

    @Autowired
    TopicRepository topicRepository;

    @MockitoBean
    ChatBroadcastService chatBroadcastService;

    private void seedProfile(String userId) {
        attendeeDataRepository.save(AttendeeDataEntity.builder()
                .userId(userId).username("Foo").furName("FooFur")
                .avatarColor("#FF0000").build());
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-1")
    void getProfile_returns200_whenExists() throws Exception {
        seedProfile("u-int-1");
        mvc.perform(get("/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("u-int-1"))
                .andExpect(jsonPath("$.data.username").value("Foo"));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-border")
    void getProfile_returnsAvatarBorderField() throws Exception {
        attendeeDataRepository.save(AttendeeDataEntity.builder()
                .userId("u-int-border").username("B").furName("BFur")
                .avatarColor("#7b9b8f").avatarBorder(true).build());
        mvc.perform(get("/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarColor").value("#7b9b8f"))
                .andExpect(jsonPath("$.data.avatarBorder").value(true));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-detail-border")
    void getProfileByUserId_returnsAvatarBorderField() throws Exception {
        attendeeDataRepository.save(AttendeeDataEntity.builder()
                .userId("u-detail-b").username("D").furName("DFur")
                .avatarColor("#c9826b").avatarBorder(true).build());
        mvc.perform(get("/user/profile/u-detail-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarColor").value("#c9826b"))
                .andExpect(jsonPath("$.data.avatarBorder").value(true));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-2")
    void getProfile_returns404_whenMissing() throws Exception {
        mvc.perform(get("/user/profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("profile_not_found"));
    }

    @Test
    void getProfile_returns401_whenUnauth() throws Exception {
        mvc.perform(get("/user/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-3")
    void postProfile_creates_withProvidedFields() throws Exception {
        String body = "{\"username\":\"小毛\",\"furName\":\"MaoMao\",\"avatarColor\":\"#FF6B6B\"}";
        mvc.perform(post("/user/profile").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value("u-int-3"))
                .andExpect(jsonPath("$.data.username").value("user-u-int-3"))
                .andExpect(jsonPath("$.data.furName").value("MaoMao"));
        assertThat(attendeeDataRepository.existsByUserId("u-int-3")).isTrue();
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-4")
    void postProfile_defaultsUsername_whenOmitted() throws Exception {
        mvc.perform(post("/user/profile").contentType(APPLICATION_JSON)
                        .content("{\"furName\":\"a\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("user-u-int-4"));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-5")
    void postProfile_returns400_whenTopicIdInvalid() throws Exception {
        mvc.perform(post("/user/profile").contentType(APPLICATION_JSON)
                        .content("{\"furName\":\"a\",\"topicId\":\"bad-id\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid_topic_id"));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-6")
    void postProfile_returns409_whenExists() throws Exception {
        seedProfile("u-int-6");
        mvc.perform(post("/user/profile").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("profile_already_exists"));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-7")
    void postProfileUpdate_updates_partialFields() throws Exception {
        seedProfile("u-int-7");
        mvc.perform(post("/user/profile/update").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"SHOULD-STAY\",\"furName\":\"NEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.furName").value("NEW"))
                .andExpect(jsonPath("$.data.username").value("Foo"));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-7b")
    void postProfileUpdate_broadcastsProfileUpdated() throws Exception {
        seedProfile("u-int-7b");
        attendeeDataRepository.findByUserId("u-int-7b").ifPresent(p -> {
            p.setAvatar("/old-avatar.png");
            attendeeDataRepository.save(p);
        });

        mvc.perform(post("/user/profile/update").contentType(APPLICATION_JSON)
                        .content("{\"furName\":\"NewFur\",\"avatar\":\"/new-avatar.png\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ChatEnvelope<?>> captor = ArgumentCaptor.forClass(ChatEnvelope.class);
        verify(chatBroadcastService, atLeastOnce()).broadcastToAll(captor.capture());
        assertThat(captor.getAllValues()).anySatisfy(envelope -> {
            assertThat(envelope.type()).isEqualTo(ChatEventType.PROFILE_UPDATED);
            assertThat(envelope.data()).isInstanceOf(ProfileUpdatedPayload.class);
            ProfileUpdatedPayload payload = (ProfileUpdatedPayload) envelope.data();
            assertThat(payload.userId()).isEqualTo("u-int-7b");
            assertThat(payload.furName()).isEqualTo("NewFur");
            assertThat(payload.avatar()).isEqualTo("/new-avatar.png");
        });
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-8")
    void postProfileUpdate_returns404_whenMissing() throws Exception {
        mvc.perform(post("/user/profile/update").contentType(APPLICATION_JSON)
                        .content("{\"furName\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("profile_not_found"));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-9")
    void getProfileByUserId_returnsNested() throws Exception {
        seedProfile("u-int-target");
        mvc.perform(get("/user/profile/u-int-target"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("u-int-target"))
                .andExpect(jsonPath("$.data.tags").isArray())
                .andExpect(jsonPath("$.data.socials").isArray())
                .andExpect(jsonPath("$.data.stickers").isArray());
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-10")
    void getProfileByUserId_returns404_whenMissing() throws Exception {
        mvc.perform(get("/user/profile/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("profile_not_found"));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-11")
    void postRedraw_returnsNewTopic_whenPoolHasOthers() throws Exception {
        seedProfile("u-int-11");
        // 隨便挑一張 seed topic 設給 user
        TopicEntity firstTopic = topicRepository.findAll().get(0);
        AttendeeDataEntity profile = attendeeDataRepository.findByUserId("u-int-11").orElseThrow();
        profile.setTopicId(firstTopic.getTopicId());
        attendeeDataRepository.save(profile);

        mvc.perform(post("/user/topic-card/redraw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").exists())
                .andExpect(jsonPath("$.data.topicId").value(not(firstTopic.getTopicId())));
    }

    @Test
    @WithMockAdmin(providerUserId = "u-int-12")
    void postRedraw_returns404_whenProfileMissing() throws Exception {
        mvc.perform(post("/user/topic-card/redraw"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("profile_not_found"));
    }
}
