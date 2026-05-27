package ru.isu.backend.dto.response;

public record TagProgressResponse(
        String name,
        Integer total,
        Integer graduated,
        Integer percent
) {
}
