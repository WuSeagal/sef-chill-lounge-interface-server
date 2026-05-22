package com.sef.cli.attendee.repository;

import com.sef.cli.attendee.entity.AttendeeDataEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Transactional
public interface AttendeeDataRepository extends JpaRepository<AttendeeDataEntity, Long> {

    Optional<AttendeeDataEntity> findByUserId(String userId);
}
