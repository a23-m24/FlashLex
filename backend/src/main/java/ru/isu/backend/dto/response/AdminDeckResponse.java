package ru.isu.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminDeckResponse(
        Long id,
        String name,
        String description,
        Long authorId,
        String authorName,
        String authorEmail,
        Boolean published,
        String level,
        List<String> tags,
        long cardCount,
        Double rating,
        Integer ratingsCount,
        Integer clonesCount,
        LocalDateTime createdAt
) {
}
