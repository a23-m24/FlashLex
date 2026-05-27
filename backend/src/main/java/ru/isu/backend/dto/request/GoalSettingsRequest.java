package ru.isu.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GoalSettingsRequest(
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
