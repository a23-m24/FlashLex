package ru.isu.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.isu.backend.dto.request.DeckRequest;
import ru.isu.backend.dto.request.UserPublicationBanRequest;
import ru.isu.backend.dto.response.AdminDashboardResponse;
import ru.isu.backend.dto.response.AdminDeckResponse;
import ru.isu.backend.dto.response.AdminUserResponse;
import ru.isu.backend.dto.response.DeckResponse;
import ru.isu.backend.exception.DuplicateResourceException;
import ru.isu.backend.exception.ForbiddenOperationException;
import ru.isu.backend.exception.NotFoundException;
import ru.isu.backend.model.Deck;
import ru.isu.backend.model.Flashcard;
import ru.isu.backend.model.User;
import ru.isu.backend.model.UserRole;
import ru.isu.backend.repository.DeckRatingRepository;
import ru.isu.backend.repository.DeckRepository;
import ru.isu.backend.repository.FlashcardRepository;
import ru.isu.backend.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final FlashcardRepository flashcardRepository;
    private final DeckRatingRepository deckRatingRepository;
    private final DeckDeletionService deckDeletionService;
    private final DeckContentService deckContentService;
    private final DeckResponseAssembler deckResponseAssembler;

    public AdminDashboardResponse getDashboard() {
        return new AdminDashboardResponse(
                userRepository.count(),
                userRepository.countByRole(UserRole.STUDENT),
                userRepository.countByRole(UserRole.ADMIN),
                userRepository.countByPublicationBannedTrue(),
                deckRepository.count(),
                deckRepository.countByPublishedTrue(),
                deckRepository.countByPublishedFalse(),
                flashcardRepository.count(),
                deckRatingRepository.count()
        );
    }

    public Page<AdminUserResponse> searchUsers(String query, Pageable pageable) {
        LocalDate today = LocalDate.now();
        Page<UserRepository.AdminUserView> users = userRepository.searchAdminUsers(
                normalizeFilter(query),
                today,
                today.minusDays(6),
                pageable
        );
        return users.map(this::toUserResponse);
    }

    public Page<AdminDeckResponse> searchDecks(String query, Pageable pageable) {
        Page<DeckRepository.AdminDeckView> decks = deckRepository.searchAdminDecks(
                normalizeFilter(query),
                pageable
        );
        List<Long> deckIds = decks.getContent().stream()
                .map(DeckRepository.AdminDeckView::getId)
                .toList();
        Map<Long, List<String>> tagsByDeckId = tagsByDeckId(deckIds);

        return new PageImpl<>(
                decks.getContent().stream()
                        .map(deck -> toDeckResponse(deck, tagsByDeckId))
                        .toList(),
                decks.getPageable(),
                decks.getTotalElements()
        );
    }

    @Transactional
    public AdminUserResponse updatePublicationBan(Long adminId, Long userId, UserPublicationBanRequest request) {
        if (adminId.equals(userId)) {
            throw new ForbiddenOperationException("Admin cannot block own publishing");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() == UserRole.ADMIN && Boolean.TRUE.equals(request.publicationBanned())) {
            throw new ForbiddenOperationException("Admin publishing cannot be blocked");
        }
        user.setPublicationBanned(Boolean.TRUE.equals(request.publicationBanned()));
        userRepository.save(user);
        LocalDate today = LocalDate.now();
        return userRepository.findAdminUserById(userId, today, today.minusDays(6))
                .map(this::toUserResponse)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public DeckResponse updatePublishedDeck(Long deckId, DeckRequest request) {
        Deck deck = findPublishedDeck(deckId);
        requireUniqueDeckName(deck.getAuthor().getId(), deckId, request.name());
        deckContentService.applyDeckFields(deck, request);
        deck.setPublished(true);
        List<Flashcard> cards = deckContentService.syncCards(deck, request);
        return deckResponseAssembler.toResponse(deck, null, cards);
    }

    @Transactional
    public void deleteDeck(Long deckId) {
        deckDeletionService.delete(findPublishedDeck(deckId));
    }

    private AdminUserResponse toUserResponse(UserRepository.AdminUserView user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDailyNewLimit(),
                user.getDailyReviewLimit(),
                Boolean.TRUE.equals(user.getPublicationBanned()),
                user.getRegisteredAt(),
                safeLong(user.getDeckCount()),
                safeLong(user.getPublishedDeckCount()),
                safeLong(user.getProgressCount()),
                safeLong(user.getTodayPoints()),
                safeLong(user.getWeekPoints())
        );
    }

    private AdminDeckResponse toDeckResponse(DeckRepository.AdminDeckView deck, Map<Long, List<String>> tagsByDeckId) {
        return new AdminDeckResponse(
                deck.getId(),
                deck.getName(),
                deck.getDescription(),
                deck.getAuthorId(),
                deck.getAuthorName(),
                deck.getAuthorEmail(),
                deck.getPublished(),
                deck.getLevel(),
                List.copyOf(tagsByDeckId.getOrDefault(deck.getId(), List.of())),
                safeLong(deck.getCardCount()),
                deck.getRating(),
                deck.getRatingsCount(),
                deck.getClonesCount(),
                deck.getCreatedAt()
        );
    }

    private Map<Long, List<String>> tagsByDeckId(List<Long> deckIds) {
        if (deckIds.isEmpty()) {
            return Map.of();
        }
        return deckRepository.findTagsByDeckIdIn(deckIds).stream()
                .collect(Collectors.groupingBy(
                        DeckRepository.DeckTagView::getDeckId,
                        Collectors.mapping(DeckRepository.DeckTagView::getTag, Collectors.toList())
                ));
    }

    private Deck findPublishedDeck(Long deckId) {
        Deck deck = deckRepository.findWithRelationsById(deckId)
                .orElseThrow(() -> new NotFoundException("Deck not found"));
        if (!Boolean.TRUE.equals(deck.getPublished())) {
            throw new ForbiddenOperationException("Admin can manage only published decks");
        }
        return deck;
    }

    private void requireUniqueDeckName(Long authorId, Long deckId, String rawName) {
        String name = rawName.trim();
        if (deckRepository.existsByAuthorIdAndNameIgnoreCaseAndIdNot(authorId, name, deckId)) {
            throw new DuplicateResourceException("Deck name already exists");
        }
    }

    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private static String normalizeFilter(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
