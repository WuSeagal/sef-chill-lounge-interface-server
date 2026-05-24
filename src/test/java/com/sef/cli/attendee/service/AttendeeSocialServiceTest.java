package com.sef.cli.attendee.service;

import com.sef.cli.api.request.AddSocialLinkRequest;
import com.sef.cli.attendee.entity.AttendeeSocialEntity;
import com.sef.cli.attendee.repository.AttendeeSocialRepository;
import com.sef.cli.common.exception.ForbiddenException;
import com.sef.cli.common.exception.SocialLinkNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendeeSocialServiceTest {

    @Mock
    AttendeeSocialRepository repo;

    @InjectMocks
    AttendeeSocialService service;

    @Test
    void addSocialLink_persists() {
        when(repo.save(any())).thenAnswer(inv -> {
            AttendeeSocialEntity e = inv.getArgument(0);
            e.setId(99L);
            return e;
        });

        AttendeeSocialEntity result = service.addSocialLink("u-1",
                new AddSocialLinkRequest("twitter", "https://twitter.com/foo"));

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getPlatform()).isEqualTo("twitter");
    }

    @Test
    void removeSocialLink_deletes_ownEntry() {
        when(repo.findById(7L)).thenReturn(Optional.of(
                AttendeeSocialEntity.builder().id(7L).userId("u-1").build()));

        service.removeSocialLink("u-1", 7L);

        verify(repo).deleteById(7L);
    }

    @Test
    void removeSocialLink_throwsForbidden_whenOtherUser() {
        when(repo.findById(7L)).thenReturn(Optional.of(
                AttendeeSocialEntity.builder().id(7L).userId("u-OTHER").build()));

        assertThatThrownBy(() -> service.removeSocialLink("u-1", 7L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void removeSocialLink_throwsSocialLinkNotFound_whenMissing() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeSocialLink("u-1", 99L))
                .isInstanceOf(SocialLinkNotFoundException.class);
    }
}
