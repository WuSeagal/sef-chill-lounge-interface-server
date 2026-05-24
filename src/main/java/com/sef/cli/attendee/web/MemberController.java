package com.sef.cli.attendee.web;

import com.sef.cli.api.MemberApi;
import com.sef.cli.api.response.MemberResponse;
import com.sef.cli.attendee.repository.AttendeeDataRepository;
import com.sef.cli.attendee.web.map.AttendeeDtoMapper;
import com.sef.cli.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController implements MemberApi {

    private final AttendeeDataRepository attendeeDataRepository;
    private final AttendeeDtoMapper mapper;

    @Override
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getAll() {
        List<MemberResponse> data = mapper.toMemberResponseList(attendeeDataRepository.findAll());
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
