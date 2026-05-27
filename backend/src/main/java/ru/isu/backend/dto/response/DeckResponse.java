package ru.isu.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record DeckResponse(
        Long id,
        String name,
        String description,
        Long authorId,
        String authorName,
        Boolean published,
        String level,
        List<String> tags,
        Double rating,
        Integer ratingsCount,
        Integer clonesCount,
        LocalDateTime createdAt,
        DeckMetricsResponse metrics,
        List<FlashcardResponse> cards
) {
}
