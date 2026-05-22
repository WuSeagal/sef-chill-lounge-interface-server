package com.sef.cli.attendee.repository;

import com.sef.cli.attendee.entity.AttendeeTagEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public interface AttendeeTagRepository extends JpaRepository<AttendeeTagEntity, Long> {

    List<AttendeeTagEntity> findByUserId(String userId);

    void deleteByUserIdAndTagId(String userId, String tagId);
}
