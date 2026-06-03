package com.sef.cli.attendee.repository;

import com.sef.cli.attendee.entity.AttendeeSocialEntity;
import com.sef.cli.attendee.enums.PlatformEnum;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public interface AttendeeSocialRepository extends JpaRepository<AttendeeSocialEntity, Long> {

    List<AttendeeSocialEntity> findByUserId(String userId);

    List<AttendeeSocialEntity> findByUserIdAndPlatform(String userId, PlatformEnum platform);
}
