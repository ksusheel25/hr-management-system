package com.company.hrsystem.auth.security;

import com.company.hrsystem.auth.entity.Role;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;

    @Value("${security.jwt.secret:replace-this-with-env-secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-seconds:3600}")
    private long expirationSeconds;

    public String generateToken(CustomUserDetails principal) {
        try {
            var now = Instant.now().getEpochSecond();
            var header = Map.of("alg", "HS256", "typ", "JWT");
            var payload = Map.of(
                    "sub", principal.getUsername(),
                    "userId", principal.getUserId().toString(),
                    "tenantId", principal.getTenantId().toString(),
                    "role", principal.getRole().name(),
                    "iat", now,
                    "exp", now + expirationSeconds);

            var encodedHeader = encode(objectMapper.writeValueAsBytes(header));
            var encodedPayload = encode(objectMapper.writeValueAsBytes(payload));
            var unsignedToken = encodedHeader + "." + encodedPayload;
            var signature = encode(sign(unsignedToken));
            return unsignedToken + "." + signature;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate JWT token", ex);
        }
    }

    public JwtClaims parseAndValidate(String token) {
        try {
            var parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }

            var unsignedToken = parts[0] + "." + parts[1];
            var expectedSignature = sign(unsignedToken);
            var providedSignature = URL_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, providedSignature)) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }

            var payloadJson = URL_DECODER.decode(parts[1]);
            var payload = objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {
            });
            var exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                throw new IllegalArgumentException("JWT token has expired");
            }

            return new JwtClaims(
                    UUID.fromString(payload.get("userId").toString()),
                    UUID.fromString(payload.get("tenantId").toString()),
                    Role.valueOf(payload.get("role").toString()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JWT token", ex);
        }
    }

    private byte[] sign(String value) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        var secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) {
        return URL_ENCODER.encodeToString(value);
    }

    public record JwtClaims(UUID userId, UUID tenantId, Role role) {
    }
}
