package com.sef.cli.tag.repository;

import com.sef.cli.tag.entity.TagEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface TagRepository extends JpaRepository<TagEntity, Long> {

    Optional<TagEntity> findByTagId(String tagId);

    List<TagEntity> findByType(String type);
}
