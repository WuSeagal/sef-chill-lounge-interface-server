package com.sef.cli.topic.service;

import com.sef.cli.common.exception.InvalidTopicIdException;
import com.sef.cli.common.exception.NoOtherTopicAvailableException;
import com.sef.cli.common.exception.NoTopicAvailableException;
import com.sef.cli.topic.entity.TopicEntity;
import com.sef.cli.topic.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    TopicRepository topicRepository;

    @InjectMocks
    TopicService topicService;

    @Test
    void pickRandom_returnsTopic_whenPoolNotEmpty() {
        TopicEntity t = TopicEntity.builder().topicId("t-1").content("hi").build();
        when(topicRepository.findRandomOne()).thenReturn(Optional.of(t));

        TopicEntity result = topicService.pickRandom();

        assertThat(result.getTopicId()).isEqualTo("t-1");
    }

    @Test
    void pickRandom_throwsNoTopicAvailable_whenEmpty() {
        when(topicRepository.findRandomOne()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.pickRandom())
                .isInstanceOf(NoTopicAvailableException.class);
    }

    @Test
    void pickRandomExcluding_returnsDifferent_whenPoolHasOthers() {
        TopicEntity t = TopicEntity.builder().topicId("t-2").content("ho").build();
        when(topicRepository.findRandomOneExcluding("t-1")).thenReturn(Optional.of(t));

        TopicEntity result = topicService.pickRandomExcluding("t-1");

        assertThat(result.getTopicId()).isEqualTo("t-2");
    }

    @Test
    void pickRandomExcluding_throwsNoOther_whenOnlyCurrentRemains() {
        when(topicRepository.findRandomOneExcluding("t-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.pickRandomExcluding("t-1"))
                .isInstanceOf(NoOtherTopicAvailableException.class);
    }

    @Test
    void findByTopicIdOrThrow_returnsTopic_whenExists() {
        TopicEntity t = TopicEntity.builder().topicId("t-3").content("x").build();
        when(topicRepository.findByTopicId("t-3")).thenReturn(Optional.of(t));

        assertThat(topicService.findByTopicIdOrThrow("t-3").getTopicId()).isEqualTo("t-3");
    }

    @Test
    void findByTopicIdOrThrow_throwsInvalidTopicId_whenMissing() {
        when(topicRepository.findByTopicId("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.findByTopicIdOrThrow("bad"))
                .isInstanceOf(InvalidTopicIdException.class);
    }
}
