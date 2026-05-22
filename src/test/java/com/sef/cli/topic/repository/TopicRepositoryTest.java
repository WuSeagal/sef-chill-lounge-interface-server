package com.sef.cli.topic.repository;

import com.sef.cli.config.JpaAuditingConfig;
import com.sef.cli.topic.entity.TopicEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class TopicRepositoryTest {

    @Autowired
    private TopicRepository topicRepository;

    @Test
    void should_save_topic() {
        TopicEntity topic = TopicEntity.builder()
                .content("你最喜歡的動物是什麼？")
                .build();

        TopicEntity saved = topicRepository.save(topic);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTopicId()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
    }

    @Test
    void should_auto_generate_topicId_via_prePersist() {
        TopicEntity topic = TopicEntity.builder()
                .content("如果可以變成任何動物你想變成什麼？")
                .build();

        TopicEntity saved = topicRepository.save(topic);

        assertThat(saved.getTopicId()).isNotNull();
        assertThat(saved.getTopicId()).isNotEmpty();
    }

    @Test
    void should_find_by_topicId() {
        TopicEntity topic = TopicEntity.builder()
                .content("最近在追什麼作品？")
                .build();
        TopicEntity saved = topicRepository.save(topic);

        Optional<TopicEntity> found = topicRepository.findByTopicId(saved.getTopicId());

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("最近在追什麼作品？");
    }

    @Test
    void should_return_empty_for_unknown_topicId() {
        Optional<TopicEntity> found = topicRepository.findByTopicId("nonexistent-topic");
        assertThat(found).isEmpty();
    }

    @Test
    void should_reject_duplicate_topicId() {
        TopicEntity first = TopicEntity.builder()
                .topicId("fixed-topic-001")
                .content("Topic 1")
                .build();
        topicRepository.saveAndFlush(first);

        TopicEntity duplicate = TopicEntity.builder()
                .topicId("fixed-topic-001")
                .content("Topic 2")
                .build();

        assertThatThrownBy(() -> topicRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_find_all_topics() {
        long initialCount = topicRepository.count();

        topicRepository.save(TopicEntity.builder().content("Topic A").build());
        topicRepository.save(TopicEntity.builder().content("Topic B").build());

        assertThat(topicRepository.findAll()).hasSize((int) initialCount + 2);
    }
}
