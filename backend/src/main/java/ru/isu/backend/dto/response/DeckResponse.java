package ru.isu.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record DeckResponse(
        Long id,
        String name,
        String description,
        Long authorId,
        String authorName,
        Long sourceDeckId,
        String sourceDeckName,
        String sourceAuthorName,
        Boolean published,
        String level,
        List<String> tags,
        Double rating,
        Integer ratingsCount,
        Long ratingTargetId,
        Integer userRating,
        Boolean canRate,
        Integer clonesCount,
        LocalDateTime createdAt,
        DeckMetricsResponse metrics,
        List<FlashcardResponse> cards
) {
}
