package ru.isu.backend.dto.response;

import java.util.List;
import java.util.Map;

public record LearningStatsResponse(
        Integer totalCards,
        Integer accuracy,
        Integer dueToday,
        DailyStatsResponse dailyStats,
        Map<String, Integer> statusSummary,
        List<WeakCardResponse> weakCards,
        List<TagProgressResponse> tagProgress
) {
}
