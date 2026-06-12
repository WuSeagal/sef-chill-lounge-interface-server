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
import com.sef.cli.tag.service.TagIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendeeTagService {

    private final TagRepository tagRepository;
    private final AttendeeTagRepository attendeeTagRepository;
    private final TagProperties tagProperties;
    private final TagIdGenerator tagIdGenerator;

    @Transactional
    public TagEntity addTag(String userId, AddTagRequest req) {
        // TOCTOU: count vs insert 之間理論上有 race window,但 tag adds 為 user-initiated
        // 低頻動作,單機部署 + 同一 user 通常單一 session,實務上影響可忽略。若未來需嚴格,
        // 可改 SERIALIZABLE isolation 或在 user 層級 row lock。
        if (attendeeTagRepository.countByUserId(userId) >= tagProperties.getMaxPerUser()) {
            log.warn("[TAG_ADD_FAIL] TAG 數量已達上限, userId={}, limit={}", userId, tagProperties.getMaxPerUser());
            throw new TagLimitExceededException();
        }
        TagEntity tag;
        if (req.getTagId() != null && !req.getTagId().isBlank()) {
            // junction-first：先檢查重複（race-safe + 少一次 DB read），再驗證 tag 存在
            if (attendeeTagRepository.existsByUserIdAndTagId(userId, req.getTagId())) {
                log.warn("[TAG_ADD_FAIL] TAG 已關聯, userId={}, tagId={}", userId, req.getTagId());
                throw new TagAlreadyAssociatedException();
            }
            // 按業務鍵 tagId 查（JPA PK 是 Long id，不能用 findById）
            tag = tagRepository.findByTagId(req.getTagId())
                    .orElseThrow(() -> new IllegalArgumentException("tag_not_found"));
        } else {
            if (req.getContent() == null || req.getContent().isBlank()) {
                throw new IllegalArgumentException("custom_tag_content_required");
            }
            // content 路徑：trim → type fallback 大寫 CUSTOM → 先查後建（同內容合併）→ dup-check 冪等
            String content = req.getContent().trim();
            String type = tagIdGenerator.normalizeType(req.getType());
            // 同內容合併：多筆命中時 is_custom=false（預設 TAG）優先、其次 id 最小，確保 deterministic
            tag = tagRepository.findByTypeAndContentNormalized(type, content).stream()
                    .min(Comparator.comparing(TagEntity::isCustom).thenComparing(TagEntity::getId))
                    .orElseGet(() -> tagRepository.save(TagEntity.builder()
                            .tagId(tagIdGenerator.generate(type))
                            .type(type).content(content).isCustom(true).build()));
            // 已持有 → 冪等成功：以既有關聯為準，不重複建 junction、回既有 TAG（D6）
            if (attendeeTagRepository.existsByUserIdAndTagId(userId, tag.getTagId())) {
                log.info("[TAG_ADD] TAG 已持有（冪等）, userId={}, tagId={}", userId, tag.getTagId());
                return tag;
            }
        }
        attendeeTagRepository.save(AttendeeTagEntity.builder()
                .userId(userId).tagId(tag.getTagId()).build());
        log.info("[TAG_ADD] TAG 新增成功, userId={}, tagId={}", userId, tag.getTagId());
        return tag;
    }

    @Transactional
    public void removeTag(String userId, String tagId) {
        attendeeTagRepository.findByUserIdAndTagId(userId, tagId)
                .orElseThrow(TagJunctionNotFoundException::new);
        attendeeTagRepository.deleteByUserIdAndTagId(userId, tagId);
        log.info("[TAG_REMOVE] TAG 移除成功, userId={}, tagId={}", userId, tagId);
    }
}
