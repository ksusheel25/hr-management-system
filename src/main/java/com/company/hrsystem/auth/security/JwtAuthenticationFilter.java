package com.company.hrsystem.auth.security;

import com.company.hrsystem.common.exception.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.hrsystem.common.context.CompanyContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/auth/login".equals(request.getServletPath())
                && "POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var token = authorization.substring(BEARER_PREFIX.length());
            var claims = jwtTokenProvider.parseAndValidate(token);
            var tokenTenantId = claims.tenantId();
            CompanyContext.setCompanyId(tokenTenantId);

            var userDetails = customUserDetailsService.loadByIdAndTenant(claims.userId(), tokenTenantId);
            if (userDetails.getRole() != claims.role()) {
                writeErrorResponse(
                        response,
                        request,
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        "Role mismatch in token");
                return;
            }

            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            writeErrorResponse(
                    response,
                    request,
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    "Invalid or expired JWT token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String error,
            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
