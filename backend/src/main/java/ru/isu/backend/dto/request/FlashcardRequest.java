package ru.isu.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.isu.backend.model.Difficulty;
import ru.isu.backend.model.PhraseType;

public record FlashcardRequest(
        Long id,

        @NotBlank
        @Size(max = 180)
        String englishWord,

        @NotBlank
        @Size(max = 240)
        String translation,

        @Size(max = 120)
        String transcription,

        @Size(max = 500)
        String exampleSentence,

        @NotNull
        PhraseType phraseType,

        @NotNull
        Difficulty difficulty
) {
}
