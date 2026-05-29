package ru.isu.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.isu.backend.dto.request.DeckRequest;
import ru.isu.backend.exception.ForbiddenOperationException;
import ru.isu.backend.mapper.DeckMapper;
import ru.isu.backend.model.Deck;
import ru.isu.backend.model.Flashcard;
import ru.isu.backend.repository.FlashcardProgressRepository;
import ru.isu.backend.repository.FlashcardRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeckContentService {

    private final FlashcardRepository flashcardRepository;
    private final FlashcardProgressRepository flashcardProgressRepository;
    private final DeckMapper deckMapper;

    public void applyDeckFields(Deck deck, DeckRequest request) {
        deck.setName(request.name().trim());
        deck.setDescription(request.description().trim());
        deck.setLevel(request.level().trim().toUpperCase());
        deck.setPublished(Boolean.TRUE.equals(request.published()));
        deck.getTags().clear();
        deck.getTags().addAll(normalizeTags(request.tags()));
    }

    public List<Flashcard> saveCards(Deck deck, DeckRequest request) {
        List<Flashcard> cards = request.cards().stream()
                .map(cardRequest -> deckMapper.toFlashcard(cardRequest, deck))
                .toList();
        return flashcardRepository.saveAll(cards);
    }

    public List<Flashcard> syncCards(Deck deck, DeckRequest request) {
        List<Flashcard> existingCards = flashcardRepository.findByDeckIdOrderByIdAsc(deck.getId());
        Map<Long, Flashcard> existingCardsById = existingCards.stream()
                .collect(Collectors.toMap(Flashcard::getId, Function.identity()));
        Set<Long> requestedExistingIds = request.cards().stream()
                .map(cardRequest -> cardRequest.id())
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        List<Long> removedCardIds = existingCards.stream()
                .map(Flashcard::getId)
                .filter(id -> !requestedExistingIds.contains(id))
                .toList();
        if (!removedCardIds.isEmpty()) {
            flashcardProgressRepository.deleteByFlashcardIdIn(removedCardIds);
            flashcardRepository.deleteByIdIn(removedCardIds);
        }

        List<Flashcard> cards = request.cards().stream()
                .map(cardRequest -> {
                    if (cardRequest.id() == null) {
                        return deckMapper.toFlashcard(cardRequest, deck);
                    }
                    Flashcard existingCard = existingCardsById.get(cardRequest.id());
                    if (existingCard == null) {
                        throw new ForbiddenOperationException("Cannot update a card from another deck");
                    }
                    deckMapper.applyFlashcardFields(cardRequest, existingCard);
                    return existingCard;
                })
                .toList();
        return flashcardRepository.saveAll(cards);
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }

        return tags.stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }
}
