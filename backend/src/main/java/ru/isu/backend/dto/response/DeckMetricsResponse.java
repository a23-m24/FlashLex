package ru.isu.backend.dto.response;

import ru.isu.backend.model.Difficulty;
import ru.isu.backend.model.PhraseType;

import java.util.Map;

public record DeckMetricsResponse(
        long cardCount,
        Map<PhraseType, Long> phraseTypes,
        Map<Difficulty, Long> difficulties
) {
}
