package ru.isu.backend.dto.response;

import java.util.List;

public record DeckCatalogFacetsResponse(
        List<String> levels,
        List<String> tags
) {
}
