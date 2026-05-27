package ru.isu.backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PhraseType {
    WORD,
    PHRASE,
    COLLOCATION,
    PHRASAL_VERB,
    IDIOM;

    @JsonCreator
    public static PhraseType fromValue(String value) {
        return value == null ? null : PhraseType.valueOf(value.trim().toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
