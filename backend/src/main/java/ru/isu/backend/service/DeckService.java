package ru.isu.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        return toResponses(deckRepository.findByAuthorIdOrderByCreatedAtDesc(userId), userId);
    }

    public Page<DeckResponse> searchPublishedDecks(
            String query,
            String level,
            String tag,
            Long currentUserId,
            Pageable pageable
    ) {
        Page<Deck> decksPage = deckRepository.searchPublished(
                normalizeFilter(query),
                normalizeFilter(level),
                normalizeFilter(tag),
                pageable
        );
        return new PageImpl<>(
                toResponses(decksPage.getContent(), currentUserId),
                decksPage.getPageable(),
                decksPage.getTotalElements()
        );
    }

    public DeckResponse getDeck(Long deckId, Long currentUserId) {
        Deck deck = findDeckWithRelations(deckId);
        if (!deck.getPublished() && (currentUserId == null || !deck.getAuthor().getId().equals(currentUserId))) {
            throw new ForbiddenOperationException("Cannot access a private deck");
        }
        return toResponse(deck, currentUserId);
    }

    @Transactional
    public DeckResponse createDeck(Long authorId, DeckRequest request) {
        User author = findUser(authorId);

        Deck deck = new Deck();
        deck.setAuthor(author);
        applyDeckFields(deck, request);

        Deck savedDeck = deckRepository.save(deck);
        List<Flashcard> cards = saveCards(savedDeck, request);

        return deckMapper.toDeckResponse(
                savedDeck,
                cards,
                Boolean.TRUE.equals(savedDeck.getPublished()) ? savedDeck : null,
                null,
                false
        );
    }

    @Transactional
    public DeckResponse updateDeck(Long userId, Long deckId, DeckRequest request) {
        Deck deck = findDeckWithRelations(deckId);
        requireOwner(deck, userId);

        applyDeckFields(deck, request);
        List<Flashcard> cards = syncCards(deck, request);

        return toResponse(deck, userId, cards);
    }

    @Transactional
    public DeckResponse publishDeck(Long userId, Long deckId) {
        Deck deck = findDeckWithRelations(deckId);
        requireOwner(deck, userId);
        deck.setPublished(true);
        return toResponse(deck, userId);
    }

    @Transactional
    public DeckResponse cloneDeck(Long userId, Long sourceDeckId) {
        User user = findUser(userId);
        Deck source = findDeckWithRelations(sourceDeckId);
        if (!source.getPublished() && !source.getAuthor().getId().equals(userId)) {
            throw new ForbiddenOperationException("Cannot clone a private deck");
        }
        if (source.getPublished() && !source.getAuthor().getId().equals(userId)) {
            Optional<Deck> existingClone = deckRepository.findFirstByAuthorIdAndSourceDeckIdOrderByCreatedAtDesc(
                    userId,
                    source.getId()
            );
            if (existingClone.isPresent()) {
                return toResponse(existingClone.get(), userId);
            }
        }

        Deck clone = new Deck();
        clone.setAuthor(user);
        clone.setSourceDeck(source.getPublished() ? source : source.getSourceDeck());
        clone.setName(source.getName());
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

        return toResponse(savedClone, userId, savedCards);
    }

    @Transactional
    public DeckResponse rateDeck(Long userId, Long deckId, RatingRequest request) {
        Deck deck = findDeckWithRelations(deckId);
        Deck ratingTarget = ratingTarget(deck);
        requireCanRate(userId, ratingTarget);

        DeckRating rating = deckRatingRepository.findByUserIdAndDeckId(userId, ratingTarget.getId())
                .orElseGet(DeckRating::new);
        rating.setUser(userRepository.getReferenceById(userId));
        rating.setDeck(ratingTarget);
        rating.setValue(request.value());
        deckRatingRepository.save(rating);

        recalculateRating(ratingTarget);

        return toResponse(deck, userId);
    }

    @Transactional
    public DeckResponse removeRating(Long userId, Long deckId) {
        Deck deck = findDeckWithRelations(deckId);
        Deck ratingTarget = ratingTarget(deck);
        requireCanRate(userId, ratingTarget);

        deckRatingRepository.deleteByUserIdAndDeckId(userId, ratingTarget.getId());
        recalculateRating(ratingTarget);

        return toResponse(deck, userId);
    }

    @Transactional
    public void deleteDeck(Long userId, Long deckId) {
        Deck deck = findDeckWithRelations(deckId);
        requireOwner(deck, userId);
        deckRatingRepository.deleteByDeckId(deckId);
        deckRepository.clearSourceDeckReferences(deckId);
        flashcardProgressRepository.deleteByFlashcardDeckId(deckId);
        flashcardRepository.deleteByDeckId(deckId);
        deckRepository.delete(deck);
    }

    private DeckResponse toResponse(Deck deck, Long currentUserId) {
        List<Flashcard> cards = flashcardRepository.findByDeckIdOrderByIdAsc(deck.getId());
        return toResponse(deck, currentUserId, cards);
    }

    private List<DeckResponse> toResponses(List<Deck> decks, Long currentUserId) {
        if (decks.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Flashcard>> cardsByDeckId = flashcardRepository.findByDeckIdInOrderByDeckIdAscIdAsc(
                        decks.stream().map(Deck::getId).toList()
                ).stream()
                .collect(Collectors.groupingBy(card -> card.getDeck().getId()));
        Map<Long, Integer> ratingsByTargetId = userRatingsByTargetId(decks, currentUserId);

        return decks.stream()
                .map(deck -> toResponse(
                        deck,
                        currentUserId,
                        cardsByDeckId.getOrDefault(deck.getId(), List.of()),
                        ratingsByTargetId
                ))
                .toList();
    }

    private DeckResponse toResponse(Deck deck, Long currentUserId, List<Flashcard> cards) {
        return toResponse(deck, currentUserId, cards, userRatingsByTargetId(List.of(deck), currentUserId));
    }

    private DeckResponse toResponse(
            Deck deck,
            Long currentUserId,
            List<Flashcard> cards,
            Map<Long, Integer> ratingsByTargetId
    ) {
        Deck ratingTarget = ratingTargetOrNull(deck);
        Integer userRating = currentUserId == null || ratingTarget == null
                ? null
                : ratingsByTargetId.get(ratingTarget.getId());
        boolean canRate = currentUserId != null
                && ratingTarget != null
                && !ratingTarget.getAuthor().getId().equals(currentUserId);
        return deckMapper.toDeckResponse(deck, cards, ratingTarget, userRating, canRate);
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

        return deckRatingRepository.findByUserIdAndDeckIdIn(currentUserId, ratingTargetIds).stream()
                .collect(Collectors.toMap(rating -> rating.getDeck().getId(), DeckRating::getValue));
    }

    private List<Flashcard> saveCards(Deck deck, DeckRequest request) {
        List<Flashcard> cards = request.cards().stream()
                .map(cardRequest -> deckMapper.toFlashcard(cardRequest, deck))
                .toList();
        return flashcardRepository.saveAll(cards);
    }

    private List<Flashcard> syncCards(Deck deck, DeckRequest request) {
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

    private Deck ratingTarget(Deck deck) {
        Deck ratingTarget = ratingTargetOrNull(deck);
        if (ratingTarget == null) {
            throw new ForbiddenOperationException("Only published catalog decks can be rated");
        }
        return ratingTarget;
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

    private void requireCanRate(Long userId, Deck ratingTarget) {
        if (ratingTarget.getAuthor().getId().equals(userId)) {
            throw new ForbiddenOperationException("Deck author cannot rate this deck");
        }
    }

    private void recalculateRating(Deck deck) {
        deck.setRating(deckRatingRepository.getAverageRating(deck.getId()));
        deck.setRatingsCount((int) deckRatingRepository.countByDeckId(deck.getId()));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Deck findDeckWithRelations(Long deckId) {
        return deckRepository.findWithRelationsById(deckId)
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
