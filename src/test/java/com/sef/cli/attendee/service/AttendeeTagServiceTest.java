package com.sef.cli.attendee.service;

import com.sef.cli.api.request.AddTagRequest;
import com.sef.cli.attendee.entity.AttendeeTagEntity;
import com.sef.cli.attendee.repository.AttendeeTagRepository;
import com.sef.cli.common.exception.TagAlreadyAssociatedException;
import com.sef.cli.common.exception.TagJunctionNotFoundException;
import com.sef.cli.tag.entity.TagEntity;
import com.sef.cli.tag.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendeeTagServiceTest {

    @Mock
    TagRepository tagRepository;

    @Mock
    AttendeeTagRepository attendeeTagRepository;

    @InjectMocks
    AttendeeTagService service;

    @Test
    void addTag_existingTagId_createsJunction() {
        when(attendeeTagRepository.existsByUserIdAndTagId("u-1", "t-existing")).thenReturn(false);
        when(tagRepository.findByTagId("t-existing")).thenReturn(Optional.of(
                TagEntity.builder().tagId("t-existing").type("default").content("foo").build()));
        when(attendeeTagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TagEntity result = service.addTag("u-1", new AddTagRequest("t-existing", null, null));

        assertThat(result.getTagId()).isEqualTo("t-existing");
        verify(attendeeTagRepository).save(argThat(j ->
                j.getUserId().equals("u-1") && j.getTagId().equals("t-existing")));
    }

    @Test
    void addTag_existingTagId_throwsDup_whenAlreadyAssociated() {
        when(attendeeTagRepository.existsByUserIdAndTagId("u-1", "t-x")).thenReturn(true);

        assertThatThrownBy(() -> service.addTag("u-1", new AddTagRequest("t-x", null, null)))
                .isInstanceOf(TagAlreadyAssociatedException.class);
    }

    @Test
    void addTag_existingTagId_throwsInvalid_whenTagMissing() {
        when(attendeeTagRepository.existsByUserIdAndTagId("u-1", "t-x")).thenReturn(false);
        when(tagRepository.findByTagId("t-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addTag("u-1", new AddTagRequest("t-x", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tag_not_found");
    }

    @Test
    void addTag_custom_createsTag_thenJunction() {
        when(tagRepository.save(any())).thenAnswer(inv -> {
            TagEntity t = inv.getArgument(0);
            t.setTagId("generated-id");
            return t;
        });
        when(attendeeTagRepository.existsByUserIdAndTagId("u-1", "generated-id")).thenReturn(false);
        when(attendeeTagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TagEntity result = service.addTag("u-1", new AddTagRequest(null, "custom", "我的標籤"));

        assertThat(result.getContent()).isEqualTo("我的標籤");
        assertThat(result.getType()).isEqualTo("custom");
    }

    @Test
    void addTag_custom_throwsIfContentBlank() {
        assertThatThrownBy(() -> service.addTag("u-1", new AddTagRequest(null, "custom", "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("custom_tag_content_required");
    }

    @Test
    void removeTag_deletesJunction() {
        when(attendeeTagRepository.findByUserIdAndTagId("u-1", "t-1")).thenReturn(
                Optional.of(AttendeeTagEntity.builder().userId("u-1").tagId("t-1").build()));

        service.removeTag("u-1", "t-1");

        verify(attendeeTagRepository).deleteByUserIdAndTagId("u-1", "t-1");
    }

    @Test
    void removeTag_throwsTagJunctionNotFound_whenMissing() {
        when(attendeeTagRepository.findByUserIdAndTagId("u-1", "no")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.removeTag("u-1", "no"))
                .isInstanceOf(TagJunctionNotFoundException.class);
    }
}
