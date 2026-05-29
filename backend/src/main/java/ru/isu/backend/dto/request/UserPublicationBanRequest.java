package ru.isu.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserPublicationBanRequest(
        @NotNull
        Boolean publicationBanned
) {
}
