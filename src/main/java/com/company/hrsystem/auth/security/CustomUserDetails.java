package com.company.hrsystem.auth.security;

import com.company.hrsystem.auth.entity.Role;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID userId;
    private final UUID tenantId;
    private final UUID employeeId;
    private final String username;
    private final String password;
    private final Role role;
    private final boolean enabled;

    public CustomUserDetails(
            UUID userId,
            UUID tenantId,
            UUID employeeId,
            String username,
            String password,
            Role role,
            boolean enabled) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.employeeId = employeeId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
