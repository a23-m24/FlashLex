package ru.isu.backend.dto.response;

public record LeaderboardRowResponse(
        Long userId,
        String name,
        Integer learnedToday,
        Integer streakDays,
        Integer accuracy,
        Integer points
) {
}
