package ru.isu.backend.dto.response;

import ru.isu.backend.model.AnswerQuality;
import ru.isu.backend.model.LearningStatus;

public record TrainingAnswerOptionResponse(
        AnswerQuality quality,
        Integer intervalMinutes,
        Integer intervalDays,
        LearningStatus nextStatus
) {
}
