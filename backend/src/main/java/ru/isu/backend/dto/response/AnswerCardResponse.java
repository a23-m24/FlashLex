package ru.isu.backend.dto.response;

public record AnswerCardResponse(
        ProgressResponse progress,
        DailyStatsResponse dailyStats
) {
}
