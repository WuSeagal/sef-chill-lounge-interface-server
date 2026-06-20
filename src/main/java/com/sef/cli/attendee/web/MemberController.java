package com.sef.cli.attendee.web;

import com.sef.cli.api.MemberApi;
import com.sef.cli.api.response.MemberResponse;
import com.sef.cli.api.response.TagResponse;
import com.sef.cli.attendee.entity.AttendeeDataEntity;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.attendee.repository.AttendeeTagRepository;
import com.sef.cli.attendee.web.map.AttendeeDtoMapper;
import com.sef.cli.common.ApiResponse;
import com.sef.cli.tag.entity.TagEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MemberController implements MemberApi {

    private static final Comparator<TagResponse> TAG_ORDER =
            Comparator.comparing(TagResponse::getType).thenComparing(TagResponse::getContent);

    private final AttendeeDataRepository attendeeDataRepository;
    private final AttendeeTagRepository attendeeTagRepository;
    private final AttendeeDtoMapper mapper;

    @Override
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getAll() {
        List<AttendeeDataEntity> attendees = attendeeDataRepository.findAll();

        // 單次批次查詢取所有 (userId, tag)，依 userId 分組，避免 per-member N+1（people-directory）。
        Map<String, List<TagResponse>> tagsByUser = new HashMap<>();
        for (Object[] row : attendeeTagRepository.findAllUserIdTagPairs()) {
            String userId = (String) row[0];
            TagEntity tag = (TagEntity) row[1];
            tagsByUser.computeIfAbsent(userId, k -> new ArrayList<>()).add(mapper.toTagResponse(tag));
        }
        tagsByUser.values().forEach(list -> list.sort(TAG_ORDER));

        List<MemberResponse> data = attendees.stream().map(entity -> {
            MemberResponse member = mapper.toMemberResponse(entity);
            member.setTags(tagsByUser.getOrDefault(entity.getUserId(), List.of()));
            return member;
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
