package ru.isu.backend.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        Integer dailyNewLimit,
        Integer dailyReviewLimit,
        LocalDateTime registeredAt
) {
}
