package com.sef.cli.attendee.service;

import com.sef.cli.api.request.AddTagRequest;
import com.sef.cli.attendee.entity.AttendeeTagEntity;
import com.sef.cli.attendee.repository.AttendeeTagRepository;
import com.sef.cli.common.exception.TagAlreadyAssociatedException;
import com.sef.cli.common.exception.TagJunctionNotFoundException;
import com.sef.cli.common.exception.TagLimitExceededException;
import com.sef.cli.tag.config.TagProperties;
import com.sef.cli.tag.entity.TagEntity;
import com.sef.cli.tag.repository.TagRepository;
import com.sef.cli.testutil.LogCaptor;
import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendeeTagServiceTest {

    @Mock
    TagRepository tagRepository;

    @Mock
    AttendeeTagRepository attendeeTagRepository;

    @Mock
    TagProperties tagProperties;

    @InjectMocks
    AttendeeTagService service;

    @BeforeEach
    void setupDefaults() {
        lenient().when(tagProperties.getMaxPerUser()).thenReturn(20);
        lenient().when(attendeeTagRepository.countByUserId(anyString())).thenReturn(0L);
    }

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
        when(tagRepository.findByTypeAndContentNormalized("CUSTOM", "我的標籤")).thenReturn(List.of());
        when(tagRepository.save(any())).thenAnswer(inv -> {
            TagEntity t = inv.getArgument(0);
            t.setTagId("generated-id");
            return t;
        });
        when(attendeeTagRepository.existsByUserIdAndTagId("u-1", "generated-id")).thenReturn(false);
        when(attendeeTagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // type 未提供 → fallback 大寫 CUSTOM；新建 TAG 必為 isCustom=true（bug fix）
        TagEntity result = service.addTag("u-1", new AddTagRequest(null, null, "我的標籤"));

        assertThat(result.getContent()).isEqualTo("我的標籤");
        assertThat(result.getType()).isEqualTo("CUSTOM");
        assertThat(result.isCustom()).isTrue();
    }

    @Test
    void addTag_content_mergesIntoExistingLowThresholdCustomTag() {
        TagEntity existing = TagEntity.builder().id(7L).tagId("custom-existing")
                .type("CUSTOM").content("私房菜").isCustom(true).build();
        when(tagRepository.findByTypeAndContentNormalized("CUSTOM", "私房菜")).thenReturn(List.of(existing));
        when(attendeeTagRepository.existsByUserIdAndTagId("u-2", "custom-existing")).thenReturn(false);
        when(attendeeTagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TagEntity result = service.addTag("u-2", new AddTagRequest(null, "CUSTOM", "私房菜"));

        assertThat(result.getTagId()).isEqualTo("custom-existing");
        verify(tagRepository, never()).save(any());
        verify(attendeeTagRepository).save(argThat(j ->
                j.getUserId().equals("u-2") && j.getTagId().equals("custom-existing")));
    }

    @Test
    void addTag_content_reusesExistingDefaultTag() {
        TagEntity def = TagEntity.builder().id(1L).tagId("L001")
                .type("LANGUAGE").content("Java").isCustom(false).build();
        when(tagRepository.findByTypeAndContentNormalized("LANGUAGE", "Java")).thenReturn(List.of(def));
        when(attendeeTagRepository.existsByUserIdAndTagId("u-3", "L001")).thenReturn(false);
        when(attendeeTagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TagEntity result = service.addTag("u-3", new AddTagRequest(null, "LANGUAGE", "Java"));

        assertThat(result.getTagId()).isEqualTo("L001");
        assertThat(result.isCustom()).isFalse();
        verify(tagRepository, never()).save(any());
    }

    @Test
    void addTag_content_multipleMatches_prefersDefaultThenMinId() {
        // custom 的 id(1) 比任何 default 都小：若只取 min id 會選到 custom，
        // 取 d-5 才證明「is_custom=false 優先」勝過「純 min id」
        TagEntity custom = TagEntity.builder().id(1L).tagId("c-1")
                .type("CUSTOM").content("露營").isCustom(true).build();
        TagEntity defNewer = TagEntity.builder().id(20L).tagId("d-20")
                .type("CUSTOM").content("露營").isCustom(false).build();
        TagEntity defOlder = TagEntity.builder().id(5L).tagId("d-5")
                .type("CUSTOM").content("露營").isCustom(false).build();
        when(tagRepository.findByTypeAndContentNormalized("CUSTOM", "露營"))
                .thenReturn(List.of(custom, defNewer, defOlder));
        when(attendeeTagRepository.existsByUserIdAndTagId("u-4", "d-5")).thenReturn(false);
        when(attendeeTagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TagEntity result = service.addTag("u-4", new AddTagRequest(null, "CUSTOM", "露營"));

        // is_custom=false 優先、其次 id 最小（default 中的 d-5）
        assertThat(result.getTagId()).isEqualTo("d-5");
        verify(tagRepository, never()).save(any());
    }

    @Test
    void addTag_content_alreadyAssociated_idempotentSuccess() {
        TagEntity def = TagEntity.builder().id(1L).tagId("L001")
                .type("LANGUAGE").content("Java").isCustom(false).build();
        when(tagRepository.findByTypeAndContentNormalized("LANGUAGE", "Java")).thenReturn(List.of(def));
        when(attendeeTagRepository.existsByUserIdAndTagId("u-1", "L001")).thenReturn(true);

        // 已持有 → content 路徑冪等成功：不拋例外、不重複建 junction、回既有 TAG（D6）
        TagEntity result = service.addTag("u-1", new AddTagRequest(null, "LANGUAGE", "Java"));

        assertThat(result.getTagId()).isEqualTo("L001");
        verify(attendeeTagRepository, never()).save(any());
        verify(tagRepository, never()).save(any());
    }

    @Test
    void addTag_content_trimsBeforeMatching() {
        TagEntity def = TagEntity.builder().id(1L).tagId("L001")
                .type("LANGUAGE").content("Java").isCustom(false).build();
        when(tagRepository.findByTypeAndContentNormalized("LANGUAGE", "Java")).thenReturn(List.of(def));
        when(attendeeTagRepository.existsByUserIdAndTagId("u-5", "L001")).thenReturn(false);
        when(attendeeTagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 傳入 "  Java  " → trim 後查詢命中既有 L001（若未 trim，repo stub 不命中而走新建）
        TagEntity result = service.addTag("u-5", new AddTagRequest(null, "LANGUAGE", "  Java  "));

        assertThat(result.getTagId()).isEqualTo("L001");
        verify(tagRepository, never()).save(any());
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

    @Test
    void addTag_throwsLimitExceeded_whenAtMax() {
        when(attendeeTagRepository.countByUserId("u-1")).thenReturn(20L);

        assertThatThrownBy(() -> service.addTag("u-1", new AddTagRequest("t-1", null, null)))
                .isInstanceOf(TagLimitExceededException.class);
    }

    // ---- 行為 log 斷言（backend-behavior-logging section 3.3）----

    @Test
    void addTag_existingTagId_logsInfo() {
        when(attendeeTagRepository.existsByUserIdAndTagId("u-log", "t-existing")).thenReturn(false);
        when(tagRepository.findByTagId("t-existing")).thenReturn(Optional.of(
                TagEntity.builder().tagId("t-existing").type("default").content("foo").build()));
        when(attendeeTagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try (LogCaptor captor = LogCaptor.forClass(AttendeeTagService.class)) {
            service.addTag("u-log", new AddTagRequest("t-existing", null, null));
            captor.assertLogged(Level.INFO, "[TAG_ADD]", "userId=u-log", "tagId=t-existing");
        }
    }

    @Test
    void removeTag_logsInfo() {
        when(attendeeTagRepository.findByUserIdAndTagId("u-log", "t-1")).thenReturn(
                Optional.of(AttendeeTagEntity.builder().userId("u-log").tagId("t-1").build()));

        try (LogCaptor captor = LogCaptor.forClass(AttendeeTagService.class)) {
            service.removeTag("u-log", "t-1");
            captor.assertLogged(Level.INFO, "[TAG_REMOVE]", "userId=u-log", "tagId=t-1");
        }
    }

    @Test
    void addTag_limitExceeded_logsWarn() {
        when(attendeeTagRepository.countByUserId("u-log")).thenReturn(20L);

        try (LogCaptor captor = LogCaptor.forClass(AttendeeTagService.class)) {
            assertThatThrownBy(() -> service.addTag("u-log", new AddTagRequest("t-1", null, null)))
                    .isInstanceOf(TagLimitExceededException.class);
            captor.assertLogged(Level.WARN, "[TAG_ADD_FAIL]", "userId=u-log");
        }
    }

    @Test
    void addTag_alreadyAssociatedById_logsWarn() {
        when(attendeeTagRepository.existsByUserIdAndTagId("u-log", "t-x")).thenReturn(true);

        try (LogCaptor captor = LogCaptor.forClass(AttendeeTagService.class)) {
            assertThatThrownBy(() -> service.addTag("u-log", new AddTagRequest("t-x", null, null)))
                    .isInstanceOf(TagAlreadyAssociatedException.class);
            captor.assertLogged(Level.WARN, "[TAG_ADD_FAIL]", "userId=u-log", "tagId=t-x");
        }
    }

    @Test
    void addTag_allowedAt19_thenBlockedAt20() {
        when(attendeeTagRepository.countByUserId("u-1")).thenReturn(19L);
        when(attendeeTagRepository.existsByUserIdAndTagId("u-1", "t-1")).thenReturn(false);
        when(tagRepository.findByTagId("t-1")).thenReturn(Optional.of(
                TagEntity.builder().tagId("t-1").type("LANGUAGE").content("Java").build()));
        when(attendeeTagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addTag("u-1", new AddTagRequest("t-1", null, null));

        when(attendeeTagRepository.countByUserId("u-1")).thenReturn(20L);
        assertThatThrownBy(() -> service.addTag("u-1", new AddTagRequest("t-2", null, null)))
                .isInstanceOf(TagLimitExceededException.class);
    }
}
