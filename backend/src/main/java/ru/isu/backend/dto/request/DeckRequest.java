package ru.isu.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DeckRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @NotBlank
        @Size(max = 1000)
        String description,

        @NotBlank
        @Size(max = 10)
        String level,

        Boolean published,

        @Size(max = 10)
        List<@NotBlank @Size(max = 50) String> tags,

        @NotEmpty
        List<@Valid FlashcardRequest> cards
) {
}
