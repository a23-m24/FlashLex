package ru.isu.backend.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.isu.backend.model.UserRole;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret:flashlex-dev-secret-change-me}") String secret,
            @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public record JwtSubject(Long userId, String email, UserRole role) {
    }

    public String generateToken(Long userId, String email, UserRole role) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", email);
        payload.put("uid", userId);
        payload.put("role", role == null ? UserRole.STUDENT.name() : role.name());
        payload.put("iat", now);
        payload.put("exp", now + expirationSeconds);

        String unsigned = encodeJson(header) + "." + encodeJson(payload);
        return unsigned + "." + sign(unsigned);
    }

    public JwtSubject readSubject(String token) {
        Map<String, Object> payload = verifyAndReadPayload(token);
        Object value = payload.get("uid");
        Long userId = null;
        if (value instanceof Number number) {
            userId = number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            userId = Long.valueOf(text);
        }
        return new JwtSubject(userId, (String) payload.get("sub"), extractRole(payload.get("role")));
    }

    private Map<String, Object> verifyAndReadPayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        String unsigned = parts[0] + "." + parts[1];
        if (!MessageDigestSafe.equals(sign(unsigned), parts[2])) {
            throw new IllegalArgumentException("Invalid JWT signature");
        }

        Map<String, Object> payload = readJson(parts[1]);
        Number expiration = (Number) payload.get("exp");
        if (expiration == null || expiration.longValue() < Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("JWT expired");
        }
        return payload;
    }

    private UserRole extractRole(Object value) {
        if (value instanceof String text && !text.isBlank()) {
            try {
                return UserRole.valueOf(text);
            } catch (IllegalArgumentException ignored) {
                return UserRole.STUDENT;
            }
        }
        return UserRole.STUDENT;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot encode JWT", exception);
        }
    }

    private Map<String, Object> readJson(String encoded) {
        try {
            byte[] json = URL_DECODER.decode(encoded);
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot decode JWT", exception);
        }
    }

    private String sign(String unsigned) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot sign JWT", exception);
        }
    }

    private static final class MessageDigestSafe {

        private MessageDigestSafe() {
        }

        static boolean equals(String left, String right) {
            byte[] a = left.getBytes(StandardCharsets.UTF_8);
            byte[] b = right.getBytes(StandardCharsets.UTF_8);
            if (a.length != b.length) {
                return false;
            }
            int result = 0;
            for (int i = 0; i < a.length; i++) {
                result |= a[i] ^ b[i];
            }
            return result == 0;
        }
    }
}
