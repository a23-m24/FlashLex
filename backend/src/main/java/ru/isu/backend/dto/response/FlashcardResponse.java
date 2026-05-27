package ru.isu.backend.dto.response;

import ru.isu.backend.model.Difficulty;
import ru.isu.backend.model.PhraseType;

public record FlashcardResponse(
        Long id,
        String englishWord,
        String translation,
        String transcription,
        String exampleSentence,
        PhraseType phraseType,
        Difficulty difficulty
) {
}
