package com.company.hrsystem.auth.security;

import com.company.hrsystem.auth.repository.AuthUserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthUserRepository authUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        throw new UsernameNotFoundException("Use tenant-aware authentication flow");
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadByTenantAndUsername(UUID tenantId, String username) {
        var authUser = authUserRepository.findByTenantIdAndUsernameIgnoreCaseAndEnabledTrue(tenantId, username)
                .orElseThrow(() -> new EntityNotFoundException("User not found in tenant context"));
        return toPrincipal(authUser);
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadByIdAndTenant(UUID userId, UUID tenantId) {
        var authUser = authUserRepository.findByIdAndTenantIdAndEnabledTrue(userId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("User not found in tenant context"));
        return toPrincipal(authUser);
    }

    private CustomUserDetails toPrincipal(com.company.hrsystem.auth.entity.AuthUser authUser) {
        return new CustomUserDetails(
                authUser.getId(),
                authUser.getTenantId(),
                authUser.getEmployeeId(),
                authUser.getUsername(),
                authUser.getPassword(),
                authUser.getRole(),
                Boolean.TRUE.equals(authUser.getEnabled()));
    }
}
