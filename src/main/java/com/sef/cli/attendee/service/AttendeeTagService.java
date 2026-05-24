package com.sef.cli.attendee.service;

import com.sef.cli.api.request.AddTagRequest;
import com.sef.cli.attendee.entity.AttendeeTagEntity;
import com.sef.cli.attendee.repository.AttendeeTagRepository;
import com.sef.cli.common.exception.TagAlreadyAssociatedException;
import com.sef.cli.common.exception.TagJunctionNotFoundException;
import com.sef.cli.tag.entity.TagEntity;
import com.sef.cli.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendeeTagService {

    private final TagRepository tagRepository;
    private final AttendeeTagRepository attendeeTagRepository;

    @Transactional
    public TagEntity addTag(String userId, AddTagRequest req) {
        TagEntity tag;
        if (req.getTagId() != null && !req.getTagId().isBlank()) {
            // junction-first：先檢查重複（race-safe + 少一次 DB read），再驗證 tag 存在
            if (attendeeTagRepository.existsByUserIdAndTagId(userId, req.getTagId())) {
                throw new TagAlreadyAssociatedException();
            }
            // 按業務鍵 tagId 查（JPA PK 是 Long id，不能用 findById）
            tag = tagRepository.findByTagId(req.getTagId())
                    .orElseThrow(() -> new IllegalArgumentException("tag_not_found"));
        } else {
            if (req.getContent() == null || req.getContent().isBlank()) {
                throw new IllegalArgumentException("custom_tag_content_required");
            }
            tag = TagEntity.builder()
                    .type(req.getType() == null ? "custom" : req.getType())
                    .content(req.getContent())
                    .build();
            tag = tagRepository.save(tag); // @PrePersist 自動產生 tagId
            if (attendeeTagRepository.existsByUserIdAndTagId(userId, tag.getTagId())) {
                throw new TagAlreadyAssociatedException();
            }
        }
        attendeeTagRepository.save(AttendeeTagEntity.builder()
                .userId(userId).tagId(tag.getTagId()).build());
        return tag;
    }

    @Transactional
    public void removeTag(String userId, String tagId) {
        attendeeTagRepository.findByUserIdAndTagId(userId, tagId)
                .orElseThrow(TagJunctionNotFoundException::new);
        attendeeTagRepository.deleteByUserIdAndTagId(userId, tagId);
    }
}
