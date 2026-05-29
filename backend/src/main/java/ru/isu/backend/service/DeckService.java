package ru.isu.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.isu.backend.dto.request.DeckRequest;
import ru.isu.backend.dto.request.RatingRequest;
import ru.isu.backend.dto.response.DeckCatalogFacetsResponse;
import ru.isu.backend.dto.response.DeckResponse;
import ru.isu.backend.exception.DuplicateResourceException;
import ru.isu.backend.exception.ForbiddenOperationException;
import ru.isu.backend.exception.NotFoundException;
import ru.isu.backend.model.Deck;
import ru.isu.backend.model.DeckRating;
import ru.isu.backend.model.Flashcard;
import ru.isu.backend.model.User;
import ru.isu.backend.model.UserRole;
import ru.isu.backend.repository.DeckRatingRepository;
import ru.isu.backend.repository.DeckRepository;
import ru.isu.backend.repository.FlashcardRepository;
import ru.isu.backend.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeckService {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final FlashcardRepository flashcardRepository;
    private final DeckRatingRepository deckRatingRepository;
    private final DeckContentService deckContentService;
    private final DeckResponseAssembler deckResponseAssembler;
    private final DeckDeletionService deckDeletionService;

    public List<DeckResponse> getMyDecks(Long userId) {
        return deckResponseAssembler.toSummaryResponses(
                deckRepository.findSummariesByAuthorIdOrderByCreatedAtDesc(userId),
                userId
        );
    }

    public DeckCatalogFacetsResponse getPublishedFacets() {
        return new DeckCatalogFacetsResponse(
                deckRepository.findPublishedLevels(),
                deckRepository.findPublishedTags()
        );
    }

    public Page<DeckResponse> searchPublishedDecks(
            String query,
            String level,
            String tag,
            Long currentUserId,
            Pageable pageable
    ) {
        Page<DeckRepository.DeckSummaryView> decksPage = deckRepository.searchPublishedSummaries(
                normalizeFilter(query),
                normalizeLevel(level),
                normalizeTag(tag),
                pageable
        );
        return new PageImpl<>(
                deckResponseAssembler.toCatalogResponses(decksPage.getContent(), currentUserId),
                decksPage.getPageable(),
                decksPage.getTotalElements()
        );
    }

    public DeckResponse getDeck(Long deckId, Long currentUserId, UserRole currentUserRole) {
        DeckRepository.DeckSummaryView deck = deckRepository.findSummaryById(deckId)
                .orElseThrow(() -> new NotFoundException("Deck not found"));
        if (!Boolean.TRUE.equals(deck.getPublished())
                && (currentUserId == null || !deck.getAuthorId().equals(currentUserId))
        ) {
            throw new ForbiddenOperationException("Cannot access a private deck");
        }
        return deckResponseAssembler.toSummaryResponse(deck, currentUserId);
    }

    @Transactional
    public DeckResponse createDeck(Long authorId, DeckRequest request) {
        User author = findUser(authorId);
        requirePublicationAllowed(author, request);
        requireUniqueDeckName(authorId, null, request.name());

        Deck deck = new Deck();
        deck.setAuthor(author);
        deckContentService.applyDeckFields(deck, request);

        Deck savedDeck = deckRepository.save(deck);
        List<Flashcard> cards = deckContentService.saveCards(savedDeck, request);

        return deckResponseAssembler.toResponse(savedDeck, authorId, cards);
    }

    @Transactional
    public DeckResponse updateDeck(Long userId, Long deckId, DeckRequest request) {
        Deck deck = findDeckWithRelations(deckId);
        requireOwner(deck, userId);
        if (!Boolean.TRUE.equals(deck.getPublished())) {
            requirePublicationAllowed(deck.getAuthor(), request);
        }
        requireUniqueDeckName(userId, deckId, request.name());

        deckContentService.applyDeckFields(deck, request);
        List<Flashcard> cards = deckContentService.syncCards(deck, request);

        return deckResponseAssembler.toResponse(deck, userId, cards);
    }

    @Transactional
    public DeckResponse publishDeck(Long userId, Long deckId) {
        Deck deck = findDeckWithRelations(deckId);
        requireOwner(deck, userId);
        requirePublicationAllowed(deck.getAuthor());
        deck.setPublished(true);
        return deckResponseAssembler.toResponse(deck, userId);
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
                return deckResponseAssembler.toResponse(existingClone.get(), userId);
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

        return deckResponseAssembler.toResponse(savedClone, userId, savedCards);
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

        return deckResponseAssembler.toResponse(deck, userId);
    }

    @Transactional
    public DeckResponse removeRating(Long userId, Long deckId) {
        Deck deck = findDeckWithRelations(deckId);
        Deck ratingTarget = ratingTarget(deck);
        requireCanRate(userId, ratingTarget);

        deckRatingRepository.deleteByUserIdAndDeckId(userId, ratingTarget.getId());
        recalculateRating(ratingTarget);

        return deckResponseAssembler.toResponse(deck, userId);
    }

    @Transactional
    public void deleteDeck(Long userId, Long deckId) {
        Deck deck = findDeckWithRelations(deckId);
        requireOwner(deck, userId);
        deckDeletionService.delete(deck);
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

    private void requireUniqueDeckName(Long authorId, Long deckId, String rawName) {
        String name = rawName.trim();
        boolean exists = deckId == null
                ? deckRepository.existsByAuthorIdAndNameIgnoreCase(authorId, name)
                : deckRepository.existsByAuthorIdAndNameIgnoreCaseAndIdNot(authorId, name, deckId);
        if (exists) {
            throw new DuplicateResourceException("Deck name already exists");
        }
    }

    private void requirePublicationAllowed(User author, DeckRequest request) {
        if (Boolean.TRUE.equals(request.published())) {
            requirePublicationAllowed(author);
        }
    }

    private void requirePublicationAllowed(User author) {
        if (Boolean.TRUE.equals(author.getPublicationBanned())) {
            throw new ForbiddenOperationException("User is blocked from publishing decks");
        }
    }

    private static String normalizeFilter(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeLevel(String value) {
        String normalized = normalizeFilter(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private static String normalizeTag(String value) {
        String normalized = normalizeFilter(value);
        return normalized == null ? null : normalized.toLowerCase();
    }
}
