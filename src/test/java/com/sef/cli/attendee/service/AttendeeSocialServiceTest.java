package com.sef.cli.attendee.service;

import com.sef.cli.api.request.AddSocialLinkRequest;
import com.sef.cli.attendee.entity.AttendeeSocialEntity;
import com.sef.cli.attendee.enums.PlatformEnum;
import com.sef.cli.attendee.repository.AttendeeSocialRepository;
import com.sef.cli.common.exception.ForbiddenException;
import com.sef.cli.common.exception.InvalidPlatformException;
import com.sef.cli.common.exception.InvalidSocialUrlException;
import com.sef.cli.common.exception.SocialLinkNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    AttendeeSocialRepository repository;

    SocialUrlValidator validator = new SocialUrlValidator();

    private AttendeeSocialService service() {
        return new AttendeeSocialService(repository, validator);
    }

    @Test
    void addsValidLinkWithEnumPlatform() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        AttendeeSocialEntity e = service().addSocialLink("u1", new AddSocialLinkRequest("X", "https://x.com/maomao"));
        assertThat(e.getPlatform()).isEqualTo(PlatformEnum.X);
        assertThat(e.getUserId()).isEqualTo("u1");
    }

    @Test
    void rejectsUnknownPlatform() {
        assertThatThrownBy(() -> service().addSocialLink("u1", new AddSocialLinkRequest("MYSPACE", "https://myspace.com/a")))
                .isInstanceOf(InvalidPlatformException.class);
    }

    @Test
    void rejectsLocalhostUrl() {
        assertThatThrownBy(() -> service().addSocialLink("u1", new AddSocialLinkRequest("PERSONAL", "http://localhost:3000")))
                .isInstanceOf(InvalidSocialUrlException.class);
    }

    @Test
    void rejectsPlatformMismatch() {
        assertThatThrownBy(() -> service().addSocialLink("u1", new AddSocialLinkRequest("GITHUB", "https://x.com/a")))
                .isInstanceOf(InvalidSocialUrlException.class);
    }

    @Test
    void removeSocialLink_deletes_ownEntry() {
        when(repository.findById(7L)).thenReturn(Optional.of(
                AttendeeSocialEntity.builder().id(7L).userId("u-1").build()));

        service().removeSocialLink("u-1", 7L);

        verify(repository).deleteById(7L);
    }

    @Test
    void removeSocialLink_throwsForbidden_whenOtherUser() {
        when(repository.findById(7L)).thenReturn(Optional.of(
                AttendeeSocialEntity.builder().id(7L).userId("u-OTHER").build()));

        assertThatThrownBy(() -> service().removeSocialLink("u-1", 7L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void removeSocialLink_throwsSocialLinkNotFound_whenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().removeSocialLink("u-1", 99L))
                .isInstanceOf(SocialLinkNotFoundException.class);
    }
}
