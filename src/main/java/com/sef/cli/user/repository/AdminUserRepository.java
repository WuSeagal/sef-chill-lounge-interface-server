package com.sef.cli.user.repository;

import com.sef.cli.user.entity.AdminUserEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@EnableJpaRepositories
@Transactional
public interface AdminUserRepository
        extends
            JpaRepository<AdminUserEntity, String>,
            JpaSpecificationExecutor<AdminUserEntity> {

    Optional<AdminUserEntity> findByProviderUserId(String providerUserId);

    boolean existsByProviderUserId(String providerUserId);

    List<AdminUserEntity> findByBannedTrue();
}
