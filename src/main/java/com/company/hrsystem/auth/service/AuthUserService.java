package com.company.hrsystem.auth.service;

import com.company.hrsystem.auth.entity.AuthUser;
import com.company.hrsystem.auth.entity.Role;
import com.company.hrsystem.auth.repository.AuthUserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthUserService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthUser createUser(
            UUID tenantId,
            String username,
            String rawPassword,
            Role role,
            UUID employeeId,
            boolean enabled) {
        if (authUserRepository.existsByTenantIdAndUsernameIgnoreCase(tenantId, username)) {
            throw new IllegalStateException("Username already exists in tenant context");
        }

        var authUser = new AuthUser();
        authUser.setCompanyId(tenantId);
        authUser.setTenantId(tenantId);
        authUser.setUsername(username.trim());
        authUser.setPassword(passwordEncoder.encode(rawPassword));
        authUser.setRole(role);
        authUser.setEmployeeId(employeeId);
        authUser.setEnabled(enabled);
        return authUserRepository.save(authUser);
    }
}
