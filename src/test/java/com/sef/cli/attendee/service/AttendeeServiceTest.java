package com.sef.cli.attendee.service;

import com.sef.cli.api.request.CreateProfileRequest;
import com.sef.cli.api.request.UpdateProfileRequest;
import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.attendee.repository.AttendeeSocialRepository;
import com.sef.cli.attendee.repository.AttendeeStickerRepository;
import com.sef.cli.attendee.repository.AttendeeTagRepository;
import com.sef.cli.common.exception.InvalidTopicIdException;
import com.sef.cli.common.exception.ProfileAlreadyExistsException;
import com.sef.cli.common.exception.ProfileNotFoundException;
import com.sef.cli.topic.entity.TopicEntity;
import com.sef.cli.topic.service.TopicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendeeServiceTest {

    @Mock
    AttendeeDataRepository attendeeDataRepository;

    @Mock
    AttendeeTagRepository attendeeTagRepository;

    @Mock
    AttendeeSocialRepository attendeeSocialRepository;

    @Mock
    AttendeeStickerRepository attendeeStickerRepository;

    @Mock
    TopicService topicService;

    @InjectMocks
    AttendeeService attendeeService;

    @Test
    void getProfileOrThrow_returnsProfile_whenExists() {
        AttendeeDataEntity entity = AttendeeDataEntity.builder()
                .userId("u-1").username("foo").build();
        when(attendeeDataRepository.findByUserId("u-1")).thenReturn(Optional.of(entity));

        assertThat(attendeeService.getProfileOrThrow("u-1").getUsername()).isEqualTo("foo");
    }

    @Test
    void getProfileOrThrow_throws_whenMissing() {
        when(attendeeDataRepository.findByUserId("u-x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> attendeeService.getProfileOrThrow("u-x"))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void createProfile_ignoresProvidedUsername_andUsesSystemUsername() {
        when(attendeeDataRepository.existsByUserId("u-1")).thenReturn(false);
        when(attendeeDataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TopicEntity topic = TopicEntity.builder().topicId("t-3").content("c").build();
        when(topicService.findByTopicIdOrThrow("t-3")).thenReturn(topic);

        CreateProfileRequest req = new CreateProfileRequest("小毛", "MaoMao", null, "#FF0", "t-3");
        AttendeeDataEntity saved = attendeeService.createProfile("u-1", req);

        assertThat(saved.getUsername()).isEqualTo("user-u-1");
        assertThat(saved.getTopicId()).isEqualTo("t-3");
    }

    @Test
    void createProfile_defaultsUsername_whenBlank() {
        when(attendeeDataRepository.existsByUserId("u-2")).thenReturn(false);
        when(attendeeDataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateProfileRequest req = new CreateProfileRequest(null, "FurOnly", null, null, null);
        AttendeeDataEntity saved = attendeeService.createProfile("u-2", req);

        assertThat(saved.getUsername()).isEqualTo("user-u-2");
        assertThat(saved.getTopicId()).isNull();
    }

    @Test
    void createProfile_throws409_whenExists() {
        when(attendeeDataRepository.existsByUserId("u-1")).thenReturn(true);

        CreateProfileRequest req = new CreateProfileRequest(null, null, null, null, null);
        assertThatThrownBy(() -> attendeeService.createProfile("u-1", req))
                .isInstanceOf(ProfileAlreadyExistsException.class);
    }

    @Test
    void createProfile_throwsInvalidTopic_whenTopicMissing() {
        when(attendeeDataRepository.existsByUserId("u-3")).thenReturn(false);
        when(topicService.findByTopicIdOrThrow("bad")).thenThrow(new InvalidTopicIdException());

        CreateProfileRequest req = new CreateProfileRequest("x", "y", null, null, "bad");
        assertThatThrownBy(() -> attendeeService.createProfile("u-3", req))
                .isInstanceOf(InvalidTopicIdException.class);
    }

    @Test
    void createProfile_defaultsAvatarColorWhite_whenOmitted() {
        when(attendeeDataRepository.existsByUserId("u-color-default")).thenReturn(false);
        when(attendeeDataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateProfileRequest req = new CreateProfileRequest(null, "Foo", null, null, null);
        AttendeeDataEntity created = attendeeService.createProfile("u-color-default", req);

        assertThat(created.getAvatarColor()).isEqualTo("#ffffff");
        assertThat(created.isAvatarBorder()).isFalse();
    }

    @Test
    void createProfile_defaultsAvatarColorWhite_whenBlank() {
        when(attendeeDataRepository.existsByUserId("u-color-blank")).thenReturn(false);
        when(attendeeDataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateProfileRequest req = new CreateProfileRequest(null, "Foo", null, "   ", null);
        AttendeeDataEntity created = attendeeService.createProfile("u-color-blank", req);

        assertThat(created.getAvatarColor()).isEqualTo("#ffffff");
    }

    @Test
    void createProfile_keepsProvidedAvatarColor() {
        when(attendeeDataRepository.existsByUserId("u-color-given")).thenReturn(false);
        when(attendeeDataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateProfileRequest req = new CreateProfileRequest(null, "Foo", null, "#7b9b8f", null);
        AttendeeDataEntity created = attendeeService.createProfile("u-color-given", req);

        assertThat(created.getAvatarColor()).isEqualTo("#7b9b8f");
    }

    @Test
    void updateProfile_updatesOnlyProvidedFields() {
        AttendeeDataEntity existing = AttendeeDataEntity.builder()
                .userId("u-1").username("old").furName("oldFur")
                .avatar("/old.png").avatarColor("#000").topicId("t-1").build();
        when(attendeeDataRepository.findByUserId("u-1")).thenReturn(Optional.of(existing));
        when(attendeeDataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest(null, "newFur", null, null, null, null);
        AttendeeDataEntity updated = attendeeService.updateProfile("u-1", req);

        assertThat(updated.getUsername()).isEqualTo("old");
        assertThat(updated.getFurName()).isEqualTo("newFur");
        assertThat(updated.getAvatar()).isEqualTo("/old.png");
    }

    @Test
    void updateProfile_ignoresUsernameChange() {
        AttendeeDataEntity existing = AttendeeDataEntity.builder()
                .userId("u-1").username("user-u-1").furName("oldFur")
                .build();
        when(attendeeDataRepository.findByUserId("u-1")).thenReturn(Optional.of(existing));
        when(attendeeDataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest("new-username", "newFur", null, null, null, null);
        AttendeeDataEntity updated = attendeeService.updateProfile("u-1", req);

        assertThat(updated.getUsername()).isEqualTo("user-u-1");
        assertThat(updated.getFurName()).isEqualTo("newFur");
    }

    @Test
    void updateProfile_updatesAvatarBorder() {
        AttendeeDataEntity existing = AttendeeDataEntity.builder()
                .userId("u-1").username("u").avatarColor("#7b9b8f").avatarBorder(false).build();
        when(attendeeDataRepository.findByUserId("u-1")).thenReturn(Optional.of(existing));
        when(attendeeDataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setAvatarBorder(true);
        AttendeeDataEntity updated = attendeeService.updateProfile("u-1", req);

        assertThat(updated.isAvatarBorder()).isTrue();
    }

    @Test
    void updateProfile_throws_whenMissing() {
        when(attendeeDataRepository.findByUserId("u-x")).thenReturn(Optional.empty());
        UpdateProfileRequest req = new UpdateProfileRequest(null, null, null, null, null, null);
        assertThatThrownBy(() -> attendeeService.updateProfile("u-x", req))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void redrawTopicCard_updatesTopicId_andReturnsNewTopic() {
        AttendeeDataEntity entity = AttendeeDataEntity.builder()
                .userId("u-1").topicId("t-1").build();
        when(attendeeDataRepository.findByUserId("u-1")).thenReturn(Optional.of(entity));
        TopicEntity newTopic = TopicEntity.builder().topicId("t-2").content("new").build();
        when(topicService.pickRandomExcluding("t-1")).thenReturn(newTopic);
        when(attendeeDataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TopicEntity result = attendeeService.redrawTopicCard("u-1");

        assertThat(result.getTopicId()).isEqualTo("t-2");
        assertThat(entity.getTopicId()).isEqualTo("t-2");
    }

    @Test
    void redrawTopicCard_picksAny_whenCurrentTopicIdNull() {
        AttendeeDataEntity entity = AttendeeDataEntity.builder()
                .userId("u-1").topicId(null).build();
        when(attendeeDataRepository.findByUserId("u-1")).thenReturn(Optional.of(entity));
        TopicEntity newTopic = TopicEntity.builder().topicId("t-99").content("x").build();
        when(topicService.pickRandom()).thenReturn(newTopic);
        when(attendeeDataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TopicEntity result = attendeeService.redrawTopicCard("u-1");
        assertThat(result.getTopicId()).isEqualTo("t-99");
    }

    @Test
    void redrawTopicCard_throws_whenProfileMissing() {
        when(attendeeDataRepository.findByUserId("u-x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> attendeeService.redrawTopicCard("u-x"))
                .isInstanceOf(ProfileNotFoundException.class);
    }
}
