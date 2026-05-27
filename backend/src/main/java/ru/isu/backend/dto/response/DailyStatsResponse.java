package ru.isu.backend.dto.response;

import java.time.LocalDate;

public record DailyStatsResponse(
        LocalDate date,
        Integer reviewed,
        Integer learned,
        Integer correct,
        Integer points,
        Integer streakDays
) {
}
