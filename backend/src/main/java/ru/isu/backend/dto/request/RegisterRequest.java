package ru.isu.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Email
        @Size(max = 160)
        String email,

        @NotBlank
        @Size(min = 6, max = 100)
        String password,

        @NotNull
        @Min(1)
        @Max(100)
        Integer dailyNewLimit,

        @NotNull
        @Min(1)
        @Max(300)
        Integer dailyReviewLimit
) {
}
