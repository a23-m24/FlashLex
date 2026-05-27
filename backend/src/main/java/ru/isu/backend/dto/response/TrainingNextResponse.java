package ru.isu.backend.dto.response;

import java.util.List;

public record TrainingNextResponse(
        Long deckId,
        FlashcardResponse card,
        ProgressResponse progress,
        Boolean finished,
        Integer dueNowCount,
        Integer newCount,
        Integer learningCount,
        Integer reviewCount,
        List<TrainingAnswerOptionResponse> answerOptions
) {
}
