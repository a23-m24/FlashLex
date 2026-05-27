package ru.isu.backend.dto.response;

public record WeakCardResponse(
        FlashcardResponse card,
        ProgressResponse progress,
        Double wrongRate,
        Integer answers
) {
}
