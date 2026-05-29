package ru.isu.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.isu.backend.dto.response.DeckMetricsResponse;
import ru.isu.backend.dto.response.DeckResponse;
import ru.isu.backend.dto.response.FlashcardResponse;
import ru.isu.backend.mapper.DeckMapper;
import ru.isu.backend.model.Difficulty;
import ru.isu.backend.model.Deck;
import ru.isu.backend.model.Flashcard;
import ru.isu.backend.model.PhraseType;
import ru.isu.backend.repository.DeckRatingRepository;
import ru.isu.backend.repository.DeckRepository;
import ru.isu.backend.repository.FlashcardRepository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DeckResponseAssembler {

    private final DeckRepository deckRepository;
    private final FlashcardRepository flashcardRepository;
    private final DeckRatingRepository deckRatingRepository;
    private final DeckMapper deckMapper;

    public DeckResponse toSummaryResponse(DeckRepository.DeckSummaryView deck, Long currentUserId) {
        List<Flashcard> cards = flashcardRepository.findByDeckIdOrderByIdAsc(deck.getId());
        Map<Long, Integer> ratingsByTargetId = userRatingsBySummaryTargetId(List.of(deck), currentUserId);
        return toSummaryResponse(
                deck,
                currentUserId,
                cards,
                tagsByDeckId(List.of(deck.getId())),
                ratingsByTargetId
        );
    }

    public List<DeckResponse> toSummaryResponses(
            List<DeckRepository.DeckSummaryView> decks,
            Long currentUserId
    ) {
        if (decks.isEmpty()) {
            return List.of();
        }

        List<Long> deckIds = decks.stream().map(DeckRepository.DeckSummaryView::getId).toList();
        Map<Long, List<Flashcard>> cardsByDeckId = flashcardRepository.findByDeckIdInOrderByDeckIdAscIdAsc(deckIds)
                .stream()
                .collect(Collectors.groupingBy(card -> card.getDeck().getId()));
        Map<Long, List<String>> tagsByDeckId = tagsByDeckId(deckIds);
        Map<Long, Integer> ratingsByTargetId = userRatingsBySummaryTargetId(decks, currentUserId);

        return decks.stream()
                .map(deck -> toSummaryResponse(
                        deck,
                        currentUserId,
                        cardsByDeckId.getOrDefault(deck.getId(), List.of()),
                        tagsByDeckId,
                        ratingsByTargetId
                ))
                .toList();
    }

    public List<DeckResponse> toCatalogResponses(
            List<DeckRepository.DeckSummaryView> decks,
            Long currentUserId
    ) {
        if (decks.isEmpty()) {
            return List.of();
        }

        List<Long> deckIds = decks.stream().map(DeckRepository.DeckSummaryView::getId).toList();
        Map<Long, List<String>> tagsByDeckId = tagsByDeckId(deckIds);
        Map<Long, Integer> ratingsByTargetId = userRatingsBySummaryTargetId(decks, currentUserId);
        Map<Long, DeckMetricsResponse> metricsByDeckId = metricsByDeckId(deckIds);

        return decks.stream()
                .map(deck -> toCatalogResponse(
                        deck,
                        currentUserId,
                        tagsByDeckId,
                        ratingsByTargetId,
                        metricsByDeckId.getOrDefault(deck.getId(), emptyMetrics())
                ))
                .toList();
    }

    public DeckResponse toResponse(Deck deck, Long currentUserId) {
        List<Flashcard> cards = flashcardRepository.findByDeckIdOrderByIdAsc(deck.getId());
        return toResponse(deck, currentUserId, cards);
    }

    public DeckResponse toResponse(Deck deck, Long currentUserId, List<Flashcard> cards) {
        Map<Long, Integer> ratingsByTargetId = userRatingsByTargetId(List.of(deck), currentUserId);
        Deck ratingTarget = ratingTargetOrNull(deck);
        Integer userRating = currentUserId == null || ratingTarget == null
                ? null
                : ratingsByTargetId.get(ratingTarget.getId());
        boolean canRate = currentUserId != null
                && ratingTarget != null
                && !ratingTarget.getAuthor().getId().equals(currentUserId);
        return deckMapper.toDeckResponse(deck, cards, ratingTarget, userRating, canRate);
    }

    private DeckResponse toSummaryResponse(
            DeckRepository.DeckSummaryView deck,
            Long currentUserId,
            List<Flashcard> cards,
            Map<Long, List<String>> tagsByDeckId,
            Map<Long, Integer> ratingsByTargetId
    ) {
        List<FlashcardResponse> cardResponses = cards.stream()
                .map(deckMapper::toFlashcardResponse)
                .toList();
        return toSummaryResponse(
                deck,
                currentUserId,
                tagsByDeckId,
                ratingsByTargetId,
                deckMapper.toMetrics(cards),
                cardResponses
        );
    }

    private DeckResponse toCatalogResponse(
            DeckRepository.DeckSummaryView deck,
            Long currentUserId,
            Map<Long, List<String>> tagsByDeckId,
            Map<Long, Integer> ratingsByTargetId,
            DeckMetricsResponse metrics
    ) {
        return toSummaryResponse(
                deck,
                currentUserId,
                tagsByDeckId,
                ratingsByTargetId,
                metrics,
                List.of()
        );
    }

    private DeckResponse toSummaryResponse(
            DeckRepository.DeckSummaryView deck,
            Long currentUserId,
            Map<Long, List<String>> tagsByDeckId,
            Map<Long, Integer> ratingsByTargetId,
            DeckMetricsResponse metrics,
            List<FlashcardResponse> cardResponses
    ) {
        Long ratingTargetId = ratingTargetId(deck);
        Integer userRating = currentUserId == null || ratingTargetId == null
                ? null
                : ratingsByTargetId.get(ratingTargetId);
        Long ratingTargetAuthorId = ratingTargetAuthorId(deck);
        boolean canRate = currentUserId != null
                && ratingTargetId != null
                && ratingTargetAuthorId != null
                && !ratingTargetAuthorId.equals(currentUserId);

        return new DeckResponse(
                deck.getId(),
                deck.getName(),
                deck.getDescription(),
                deck.getAuthorId(),
                deck.getAuthorName(),
                deck.getSourceDeckId(),
                deck.getSourceDeckName(),
                deck.getSourceAuthorName(),
                deck.getPublished(),
                deck.getLevel(),
                List.copyOf(tagsByDeckId.getOrDefault(deck.getId(), List.of())),
                ratingTargetRating(deck),
                ratingTargetRatingsCount(deck),
                ratingTargetId,
                userRating,
                canRate,
                deck.getClonesCount(),
                deck.getCreatedAt(),
                metrics,
                cardResponses
        );
    }

    private Map<Long, Integer> userRatingsByTargetId(List<Deck> decks, Long currentUserId) {
        if (currentUserId == null || decks.isEmpty()) {
            return Map.of();
        }

        List<Long> ratingTargetIds = decks.stream()
                .map(this::ratingTargetOrNull)
                .filter(target -> target != null)
                .map(Deck::getId)
                .distinct()
                .toList();
        if (ratingTargetIds.isEmpty()) {
            return Map.of();
        }

        return deckRatingRepository.findValuesByUserIdAndDeckIdIn(currentUserId, ratingTargetIds).stream()
                .collect(Collectors.toMap(
                        DeckRatingRepository.UserDeckRatingView::getDeckId,
                        DeckRatingRepository.UserDeckRatingView::getValue
                ));
    }

    private Map<Long, Integer> userRatingsBySummaryTargetId(
            List<DeckRepository.DeckSummaryView> decks,
            Long currentUserId
    ) {
        if (currentUserId == null || decks.isEmpty()) {
            return Map.of();
        }

        List<Long> ratingTargetIds = decks.stream()
                .map(this::ratingTargetId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ratingTargetIds.isEmpty()) {
            return Map.of();
        }

        return deckRatingRepository.findValuesByUserIdAndDeckIdIn(currentUserId, ratingTargetIds).stream()
                .collect(Collectors.toMap(
                        DeckRatingRepository.UserDeckRatingView::getDeckId,
                        DeckRatingRepository.UserDeckRatingView::getValue
                ));
    }

    private Map<Long, List<String>> tagsByDeckId(List<Long> deckIds) {
        return deckRepository.findTagsByDeckIdIn(deckIds).stream()
                .collect(Collectors.groupingBy(
                        DeckRepository.DeckTagView::getDeckId,
                        Collectors.mapping(DeckRepository.DeckTagView::getTag, Collectors.toList())
                ));
    }

    private Map<Long, DeckMetricsResponse> metricsByDeckId(List<Long> deckIds) {
        return flashcardRepository.findMetricsByDeckIdIn(deckIds).stream()
                .collect(Collectors.toMap(
                        FlashcardRepository.DeckMetricsView::getDeckId,
                        this::toMetrics
                ));
    }

    private DeckMetricsResponse toMetrics(FlashcardRepository.DeckMetricsView metrics) {
        Map<PhraseType, Long> phraseTypes = emptyPhraseTypeCounts();
        phraseTypes.put(PhraseType.WORD, safeLong(metrics.getWordCount()));
        phraseTypes.put(PhraseType.COLLOCATION, safeLong(metrics.getCollocationCount()));
        phraseTypes.put(PhraseType.PHRASAL_VERB, safeLong(metrics.getPhrasalVerbCount()));
        phraseTypes.put(PhraseType.IDIOM, safeLong(metrics.getIdiomCount()));
        phraseTypes.put(PhraseType.PHRASE, safeLong(metrics.getPhraseCount()));

        Map<Difficulty, Long> difficulties = emptyDifficultyCounts();
        difficulties.put(Difficulty.EASY, safeLong(metrics.getEasyCount()));
        difficulties.put(Difficulty.MEDIUM, safeLong(metrics.getMediumCount()));
        difficulties.put(Difficulty.HARD, safeLong(metrics.getHardCount()));

        return new DeckMetricsResponse(safeLong(metrics.getCardCount()), phraseTypes, difficulties);
    }

    private DeckMetricsResponse emptyMetrics() {
        return new DeckMetricsResponse(0, emptyPhraseTypeCounts(), emptyDifficultyCounts());
    }

    private Map<PhraseType, Long> emptyPhraseTypeCounts() {
        Map<PhraseType, Long> counts = new EnumMap<>(PhraseType.class);
        for (PhraseType type : PhraseType.values()) {
            counts.put(type, 0L);
        }
        return counts;
    }

    private Map<Difficulty, Long> emptyDifficultyCounts() {
        Map<Difficulty, Long> counts = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            counts.put(difficulty, 0L);
        }
        return counts;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private Deck ratingTargetOrNull(Deck deck) {
        if (deck.getPublished()) {
            return deck;
        }
        Deck sourceDeck = deck.getSourceDeck();
        if (sourceDeck != null && sourceDeck.getPublished()) {
            return sourceDeck;
        }
        return null;
    }

    private Long ratingTargetId(DeckRepository.DeckSummaryView deck) {
        if (Boolean.TRUE.equals(deck.getPublished())) {
            return deck.getId();
        }
        if (deck.getSourceDeckId() != null && Boolean.TRUE.equals(deck.getSourceDeckPublished())) {
            return deck.getSourceDeckId();
        }
        return null;
    }

    private Long ratingTargetAuthorId(DeckRepository.DeckSummaryView deck) {
        if (Boolean.TRUE.equals(deck.getPublished())) {
            return deck.getAuthorId();
        }
        if (deck.getSourceDeckId() != null && Boolean.TRUE.equals(deck.getSourceDeckPublished())) {
            return deck.getSourceAuthorId();
        }
        return null;
    }

    private Double ratingTargetRating(DeckRepository.DeckSummaryView deck) {
        if (Boolean.TRUE.equals(deck.getPublished())) {
            return deck.getRating();
        }
        if (deck.getSourceDeckId() != null && Boolean.TRUE.equals(deck.getSourceDeckPublished())) {
            return deck.getSourceDeckRating();
        }
        return deck.getRating();
    }

    private Integer ratingTargetRatingsCount(DeckRepository.DeckSummaryView deck) {
        if (Boolean.TRUE.equals(deck.getPublished())) {
            return deck.getRatingsCount();
        }
        if (deck.getSourceDeckId() != null && Boolean.TRUE.equals(deck.getSourceDeckPublished())) {
            return deck.getSourceDeckRatingsCount();
        }
        return deck.getRatingsCount();
    }
}
