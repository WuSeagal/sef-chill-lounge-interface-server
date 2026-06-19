package com.sef.cli.blacklist.service;

import com.sef.cli.api.response.BlacklistEntryResponse;
import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.attendee.web.map.BlacklistDtoMapper;
import com.sef.cli.common.HostAuthz;
import com.sef.cli.user.entity.AdminUserEntity;
import com.sef.cli.user.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class BlacklistServiceTest {

    private AdminUserRepository adminUserRepository;
    private AttendeeDataRepository attendeeDataRepository;
    private BlacklistService service;

    @BeforeEach
    void setUp() {
        adminUserRepository = mock(AdminUserRepository.class);
        attendeeDataRepository = mock(AttendeeDataRepository.class);
        BlacklistDtoMapper mapper = Mappers.getMapper(BlacklistDtoMapper.class);
        service = new BlacklistService(adminUserRepository, attendeeDataRepository, mapper);
    }

    private AdminUserEntity admin(String providerUserId, boolean banned) {
        return AdminUserEntity.builder()
                .providerUserId(providerUserId)
                .roleName("ROLE_USER").enabled(true).firstLogin(false)
                .banned(banned).build();
    }

    @Test
    void ban_setsBannedTrue_andReturnsTrue() {
        AdminUserEntity user = admin("u-1", false);
        when(adminUserRepository.findByProviderUserId("u-1")).thenReturn(Optional.of(user));

        boolean ok = service.ban("u-1");

        assertThat(ok).isTrue();
        ArgumentCaptor<AdminUserEntity> captor = ArgumentCaptor.forClass(AdminUserEntity.class);
        verify(adminUserRepository).save(captor.capture());
        assertThat(captor.getValue().getBanned()).isTrue();
    }

    @Test
    void ban_unknownUserId_returnsFalse_andDoesNotSave() {
        when(adminUserRepository.findByProviderUserId("ghost")).thenReturn(Optional.empty());

        boolean ok = service.ban("ghost");

        assertThat(ok).isFalse();
        verify(adminUserRepository, never()).save(any());
    }

    @Test
    void ban_host_returnsFalse_andDoesNotSave() {
        // 究極保護：host 不可被封禁，service 直接擋下、不查 DB、不存檔
        boolean ok = service.ban(HostAuthz.HOST_PROVIDER_USER_ID);

        assertThat(ok).isFalse();
        verify(adminUserRepository, never()).findByProviderUserId(anyString());
        verify(adminUserRepository, never()).save(any());
    }

    @Test
    void unban_setsBannedFalse() {
        AdminUserEntity user = admin("u-1", true);
        when(adminUserRepository.findByProviderUserId("u-1")).thenReturn(Optional.of(user));

        service.unban("u-1");

        ArgumentCaptor<AdminUserEntity> captor = ArgumentCaptor.forClass(AdminUserEntity.class);
        verify(adminUserRepository).save(captor.capture());
        assertThat(captor.getValue().getBanned()).isFalse();
    }

    @Test
    void unban_unknownUserId_doesNotSave() {
        when(adminUserRepository.findByProviderUserId("ghost")).thenReturn(Optional.empty());

        service.unban("ghost");

        verify(adminUserRepository, never()).save(any());
    }

    @Test
    void list_returnsBannedUsersJoinedWithAttendeeDisplayNames() {
        when(adminUserRepository.findByBannedTrue()).thenReturn(List.of(
                admin("u-1", true), admin("u-2", true)));
        when(attendeeDataRepository.findByUserIdIn(any())).thenReturn(List.of(
                AttendeeDataEntity.builder().userId("u-1").furName("Fox").username("foxxy").build(),
                AttendeeDataEntity.builder().userId("u-2").furName("Wolf").username("wolfie").build()));

        List<BlacklistEntryResponse> result = service.list();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(BlacklistEntryResponse::getUserId)
                .containsExactlyInAnyOrder("u-1", "u-2");
        assertThat(result).extracting(BlacklistEntryResponse::getFurName)
                .containsExactlyInAnyOrder("Fox", "Wolf");
        assertThat(result).extracting(BlacklistEntryResponse::getUsername)
                .containsExactlyInAnyOrder("foxxy", "wolfie");
    }

    @Test
    void list_empty_whenNoBannedUsers() {
        when(adminUserRepository.findByBannedTrue()).thenReturn(List.of());

        List<BlacklistEntryResponse> result = service.list();

        assertThat(result).isEmpty();
        verify(attendeeDataRepository, never()).findByUserIdIn(any());
    }
}
