package ru.isu.backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Difficulty {
    EASY,
    MEDIUM,
    HARD;

    @JsonCreator
    public static Difficulty fromValue(String value) {
        return value == null ? null : Difficulty.valueOf(value.trim().toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
