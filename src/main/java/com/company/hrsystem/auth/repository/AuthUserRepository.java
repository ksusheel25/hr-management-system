package com.company.hrsystem.auth.repository;

import com.company.hrsystem.auth.entity.AuthUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {

    Optional<AuthUser> findByTenantIdAndUsernameIgnoreCaseAndEnabledTrue(UUID tenantId, String username);

    Optional<AuthUser> findByIdAndTenantIdAndEnabledTrue(UUID id, UUID tenantId);

    boolean existsByTenantIdAndUsernameIgnoreCase(UUID tenantId, String username);
}
