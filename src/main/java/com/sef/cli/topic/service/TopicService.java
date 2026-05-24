package com.sef.cli.topic.service;

import com.sef.cli.common.exception.InvalidTopicIdException;
import com.sef.cli.common.exception.NoOtherTopicAvailableException;
import com.sef.cli.common.exception.NoTopicAvailableException;
import com.sef.cli.topic.entity.TopicEntity;
import com.sef.cli.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicEntity pickRandom() {
        return topicRepository.findRandomOne()
                .orElseThrow(NoTopicAvailableException::new);
    }

    public TopicEntity pickRandomExcluding(String currentTopicId) {
        return topicRepository.findRandomOneExcluding(currentTopicId)
                .orElseThrow(NoOtherTopicAvailableException::new);
    }

    public TopicEntity findByTopicIdOrThrow(String topicId) {
        return topicRepository.findByTopicId(topicId)
                .orElseThrow(InvalidTopicIdException::new);
    }
}
