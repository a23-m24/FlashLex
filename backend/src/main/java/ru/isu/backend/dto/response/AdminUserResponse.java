package ru.isu.backend.dto.response;

import ru.isu.backend.model.UserRole;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        Integer dailyNewLimit,
        Integer dailyReviewLimit,
        Boolean publicationBanned,
        LocalDateTime registeredAt,
        long deckCount,
        long publishedDeckCount,
        long progressCount,
        long todayPoints,
        long weekPoints
) {
}
