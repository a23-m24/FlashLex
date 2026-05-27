package ru.isu.backend.dto.response;

public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user
) {
}
