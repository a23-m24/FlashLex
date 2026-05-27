package ru.isu.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.isu.backend.dto.request.DeckRequest;
import ru.isu.backend.dto.request.RatingRequest;
import ru.isu.backend.dto.response.DeckResponse;
import ru.isu.backend.exception.ForbiddenOperationException;
import ru.isu.backend.exception.NotFoundException;
import ru.isu.backend.mapper.DeckMapper;
import ru.isu.backend.model.Deck;
import ru.isu.backend.model.DeckRating;
import ru.isu.backend.model.Flashcard;
import ru.isu.backend.model.User;
import ru.isu.backend.repository.DeckRatingRepository;
import ru.isu.backend.repository.DeckRepository;
import ru.isu.backend.repository.FlashcardProgressRepository;
import ru.isu.backend.repository.FlashcardRepository;
import ru.isu.backend.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeckService {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final FlashcardRepository flashcardRepository;
    private final DeckRatingRepository deckRatingRepository;
    private final FlashcardProgressRepository flashcardProgressRepository;
    private final DeckMapper deckMapper;

    public List<DeckResponse> getMyDecks(Long userId) {
        return deckRepository.findByAuthorIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<DeckResponse> searchPublishedDecks(
            String query,
            String level,
            String tag,
            Pageable pageable
    ) {
        return deckRepository.searchPublished(
                normalizeFilter(query),
                normalizeFilter(level),
                normalizeFilter(tag),
                pageable
        ).map(this::toResponse);
    }

    public DeckResponse getDeck(Long deckId, Long currentUserId) {
        Deck deck = findDeck(deckId);
        if (!deck.getPublished() && (currentUserId == null || !deck.getAuthor().getId().equals(currentUserId))) {
            throw new ForbiddenOperationException("Cannot access a private deck");
        }
        return toResponse(deck);
    }

    @Transactional
    public DeckResponse createDeck(Long authorId, DeckRequest request) {
        User author = findUser(authorId);

        Deck deck = new Deck();
        deck.setAuthor(author);
        applyDeckFields(deck, request);

        Deck savedDeck = deckRepository.save(deck);
        List<Flashcard> cards = saveCards(savedDeck, request);

        return deckMapper.toDeckResponse(savedDeck, cards);
    }

    @Transactional
    public DeckResponse updateDeck(Long userId, Long deckId, DeckRequest request) {
        Deck deck = findDeck(deckId);
        requireOwner(deck, userId);

        applyDeckFields(deck, request);
        flashcardProgressRepository.deleteByFlashcardDeckId(deck.getId());
        flashcardRepository.deleteByDeckId(deck.getId());
        List<Flashcard> cards = saveCards(deck, request);

        return deckMapper.toDeckResponse(deck, cards);
    }

    @Transactional
    public DeckResponse publishDeck(Long userId, Long deckId) {
        Deck deck = findDeck(deckId);
        requireOwner(deck, userId);
        deck.setPublished(true);
        return toResponse(deck);
    }

    @Transactional
    public DeckResponse cloneDeck(Long userId, Long sourceDeckId) {
        User user = findUser(userId);
        Deck source = findDeck(sourceDeckId);
        if (!source.getPublished() && !source.getAuthor().getId().equals(userId)) {
            throw new ForbiddenOperationException("Cannot clone a private deck");
        }

        Deck clone = new Deck();
        clone.setAuthor(user);
        clone.setName(source.getName() + " - copy");
        clone.setDescription(source.getDescription());
        clone.setPublished(false);
        clone.setLevel(source.getLevel());
        clone.setTags(new ArrayList<>(source.getTags()));
        clone.setRating(0.0);
        clone.setRatingsCount(0);
        clone.setClonesCount(0);

        source.setClonesCount(source.getClonesCount() + 1);
        Deck savedClone = deckRepository.save(clone);

        List<Flashcard> sourceCards = flashcardRepository.findByDeckIdOrderByIdAsc(sourceDeckId);
        List<Flashcard> clonedCards = sourceCards.stream()
                .map(card -> copyCard(card, savedClone))
                .toList();
        List<Flashcard> savedCards = flashcardRepository.saveAll(clonedCards);

        return deckMapper.toDeckResponse(savedClone, savedCards);
    }

    @Transactional
    public DeckResponse rateDeck(Long userId, Long deckId, RatingRequest request) {
        User user = findUser(userId);
        Deck deck = findDeck(deckId);
        if (!deck.getPublished()) {
            throw new ForbiddenOperationException("Only published decks can be rated");
        }

        DeckRating rating = deckRatingRepository.findByUserIdAndDeckId(userId, deckId)
                .orElseGet(DeckRating::new);
        rating.setUser(user);
        rating.setDeck(deck);
        rating.setValue(request.value());
        deckRatingRepository.save(rating);

        deck.setRating(deckRatingRepository.getAverageRating(deckId));
        deck.setRatingsCount((int) deckRatingRepository.countByDeckId(deckId));

        return toResponse(deck);
    }

    @Transactional
    public void deleteDeck(Long userId, Long deckId) {
        Deck deck = findDeck(deckId);
        requireOwner(deck, userId);
        deckRatingRepository.deleteByDeckId(deckId);
        flashcardProgressRepository.deleteByFlashcardDeckId(deckId);
        flashcardRepository.deleteByDeckId(deckId);
        deckRepository.delete(deck);
    }

    private DeckResponse toResponse(Deck deck) {
        List<Flashcard> cards = flashcardRepository.findByDeckIdOrderByIdAsc(deck.getId());
        return deckMapper.toDeckResponse(deck, cards);
    }

    private List<Flashcard> saveCards(Deck deck, DeckRequest request) {
        List<Flashcard> cards = request.cards().stream()
                .map(cardRequest -> deckMapper.toFlashcard(cardRequest, deck))
                .toList();
        return flashcardRepository.saveAll(cards);
    }

    private void applyDeckFields(Deck deck, DeckRequest request) {
        deck.setName(request.name().trim());
        deck.setDescription(request.description().trim());
        deck.setLevel(request.level().trim().toUpperCase());
        deck.setPublished(Boolean.TRUE.equals(request.published()));
        deck.getTags().clear();
        deck.getTags().addAll(normalizeTags(request.tags()));
    }

    private Flashcard copyCard(Flashcard source, Deck targetDeck) {
        Flashcard clone = new Flashcard();
        clone.setDeck(targetDeck);
        clone.setEnglishWord(source.getEnglishWord());
        clone.setTranslation(source.getTranslation());
        clone.setTranscription(source.getTranscription());
        clone.setExampleSentence(source.getExampleSentence());
        clone.setPhraseType(source.getPhraseType());
        clone.setDifficulty(source.getDifficulty());
        return clone;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Deck findDeck(Long deckId) {
        return deckRepository.findById(deckId)
                .orElseThrow(() -> new NotFoundException("Deck not found"));
    }

    private void requireOwner(Deck deck, Long userId) {
        if (!deck.getAuthor().getId().equals(userId)) {
            throw new ForbiddenOperationException("Only deck owner can perform this action");
        }
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

    private static String normalizeFilter(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
