package ru.isu.backend.mapper;

import org.springframework.stereotype.Component;
import ru.isu.backend.dto.request.FlashcardRequest;
import ru.isu.backend.dto.response.DeckMetricsResponse;
import ru.isu.backend.dto.response.DeckResponse;
import ru.isu.backend.dto.response.FlashcardResponse;
import ru.isu.backend.model.Deck;
import ru.isu.backend.model.Difficulty;
import ru.isu.backend.model.Flashcard;
import ru.isu.backend.model.PhraseType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DeckMapper {

    public Flashcard toFlashcard(FlashcardRequest request, Deck deck) {
        Flashcard flashcard = new Flashcard();
        flashcard.setDeck(deck);
        flashcard.setEnglishWord(request.englishWord().trim());
        flashcard.setTranslation(request.translation().trim());
        flashcard.setTranscription(trimToNull(request.transcription()));
        flashcard.setExampleSentence(trimToNull(request.exampleSentence()));
        flashcard.setPhraseType(request.phraseType());
        flashcard.setDifficulty(request.difficulty());
        return flashcard;
    }

    public FlashcardResponse toFlashcardResponse(Flashcard flashcard) {
        return new FlashcardResponse(
                flashcard.getId(),
                flashcard.getEnglishWord(),
                flashcard.getTranslation(),
                flashcard.getTranscription(),
                flashcard.getExampleSentence(),
                flashcard.getPhraseType(),
                flashcard.getDifficulty()
        );
    }

    public DeckResponse toDeckResponse(Deck deck, List<Flashcard> cards) {
        List<FlashcardResponse> cardResponses = cards.stream()
                .map(this::toFlashcardResponse)
                .toList();

        return new DeckResponse(
                deck.getId(),
                deck.getName(),
                deck.getDescription(),
                deck.getAuthor().getId(),
                deck.getAuthor().getName(),
                deck.getPublished(),
                deck.getLevel(),
                List.copyOf(deck.getTags()),
                deck.getRating(),
                deck.getRatingsCount(),
                deck.getClonesCount(),
                deck.getCreatedAt(),
                toMetrics(cards),
                cardResponses
        );
    }

    public DeckMetricsResponse toMetrics(List<Flashcard> cards) {
        Map<PhraseType, Long> phraseTypes = initializeEnumCounts(PhraseType.class);
        cards.stream()
                .collect(Collectors.groupingBy(Flashcard::getPhraseType, Collectors.counting()))
                .forEach(phraseTypes::put);

        Map<Difficulty, Long> difficulties = initializeEnumCounts(Difficulty.class);
        cards.stream()
                .collect(Collectors.groupingBy(Flashcard::getDifficulty, Collectors.counting()))
                .forEach(difficulties::put);

        return new DeckMetricsResponse(cards.size(), phraseTypes, difficulties);
    }

    private static <E extends Enum<E>> Map<E, Long> initializeEnumCounts(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .collect(Collectors.toMap(Function.identity(), ignored -> 0L));
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
