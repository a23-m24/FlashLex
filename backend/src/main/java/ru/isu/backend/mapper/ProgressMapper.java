package ru.isu.backend.mapper;

import org.springframework.stereotype.Component;
import ru.isu.backend.dto.response.DailyStatsResponse;
import ru.isu.backend.dto.response.ProgressResponse;
import ru.isu.backend.model.DailyUserStats;
import ru.isu.backend.model.FlashcardProgress;

@Component
public class ProgressMapper {

    public ProgressResponse toProgressResponse(FlashcardProgress progress) {
        return new ProgressResponse(
                progress.getId(),
                progress.getUser().getId(),
                progress.getFlashcard().getId(),
                progress.getStatus(),
                progress.getIntervalDays(),
                progress.getIntervalMinutes(),
                progress.getIntervalSeconds(),
                progress.getEaseFactor(),
                progress.getNextReviewDate(),
                progress.getNextReviewAt(),
                progress.getCorrectAnswers(),
                progress.getWrongAnswers(),
                progress.getRemainingSteps(),
                progress.getLapseCount(),
                progress.getLeeched(),
                progress.getLastReviewedAt(),
                progress.getLastAnswerQuality()
        );
    }

    public DailyStatsResponse toDailyStatsResponse(DailyUserStats stats) {
        return new DailyStatsResponse(
                stats.getDate(),
                stats.getReviewed(),
                stats.getLearned(),
                stats.getCorrect(),
                stats.getPoints(),
                stats.getStreakDays()
        );
    }
}
