package com.sef.cli.topic.repository;

import com.sef.cli.topic.entity.TopicEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Transactional
public interface TopicRepository extends JpaRepository<TopicEntity, Long> {

    Optional<TopicEntity> findByTopicId(String topicId);

    @Query(value = "SELECT * FROM topic ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<TopicEntity> findRandomOne();

    @Query(value = "SELECT * FROM topic WHERE topic_id <> :excludeId ORDER BY RANDOM() LIMIT 1",
           nativeQuery = true)
    Optional<TopicEntity> findRandomOneExcluding(@Param("excludeId") String excludeId);
}
