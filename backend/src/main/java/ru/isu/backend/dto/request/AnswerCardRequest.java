package ru.isu.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.isu.backend.model.AnswerQuality;

public record AnswerCardRequest(
        @NotNull
        @Positive
        Long flashcardId,

        @NotNull
        AnswerQuality quality
) {
}
