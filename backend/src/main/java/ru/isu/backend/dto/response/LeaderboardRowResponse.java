package ru.isu.backend.dto.response;

public record LeaderboardRowResponse(
        Long userId,
        String name,
        Integer learnedToday,
        Integer reviewed,
        Integer extraNew,
        Integer extraReview,
        Integer streakDays,
        Integer accuracy,
        Integer points
) {
}
