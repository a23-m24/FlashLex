package ru.isu.backend.dto.response;

import ru.isu.backend.model.AnswerQuality;
import ru.isu.backend.model.LearningStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProgressResponse(
        Long id,
        Long userId,
        Long flashcardId,
        LearningStatus status,
        Integer intervalDays,
        Integer intervalMinutes,
        Long intervalSeconds,
        Double easeFactor,
        LocalDate nextReviewDate,
        LocalDateTime nextReviewAt,
        Integer correctAnswers,
        Integer wrongAnswers,
        Integer remainingSteps,
        Integer lapseCount,
        Boolean leeched,
        LocalDateTime lastReviewedAt,
        AnswerQuality lastAnswerQuality
) {
}
