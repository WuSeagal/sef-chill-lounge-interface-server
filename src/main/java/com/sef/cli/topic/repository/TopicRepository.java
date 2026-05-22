package com.sef.cli.topic.repository;

import com.sef.cli.topic.entity.TopicEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Transactional
public interface TopicRepository extends JpaRepository<TopicEntity, Long> {

    Optional<TopicEntity> findByTopicId(String topicId);
}
