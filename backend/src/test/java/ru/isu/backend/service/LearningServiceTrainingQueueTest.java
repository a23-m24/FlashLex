package ru.isu.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.isu.backend.dto.response.TrainingNextResponse;
import ru.isu.backend.mapper.DeckMapper;
import ru.isu.backend.mapper.ProgressMapper;
import ru.isu.backend.model.Deck;
import ru.isu.backend.model.Flashcard;
import ru.isu.backend.model.FlashcardProgress;
import ru.isu.backend.model.LearningStatus;
import ru.isu.backend.model.User;
import ru.isu.backend.repository.DailyUserStatsRepository;
import ru.isu.backend.repository.DeckRepository;
import ru.isu.backend.repository.FlashcardProgressRepository;
import ru.isu.backend.repository.FlashcardRepository;
import ru.isu.backend.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningServiceTrainingQueueTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private FlashcardProgressRepository progressRepository;

    @Mock
    private DailyUserStatsRepository statsRepository;

    private LearningService service;

    @BeforeEach
    void setUp() {
        service = new LearningService(
                userRepository,
                deckRepository,
                flashcardRepository,
                progressRepository,
                statsRepository,
                new ProgressMapper(),
                new DeckMapper(),
                new CyclicTrainingScheduler()
        );
    }

    @Test
    void fallsBackToLearningCardsWhenNewAndDueReviewCardsAreEmpty() {
        User user = user(1L);
        Deck deck = deck(10L, user);
        Flashcard card = card(100L, deck);
        FlashcardProgress learning = progress(
                1000L,
                user,
                card,
                LearningStatus.LEARNING,
                LocalDateTime.now().plusMinutes(10)
        );

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(flashcardRepository.findNewCardsInDeck(user.getId(), deck.getId())).thenReturn(List.of());
        when(progressRepository.findByUserAndDeck(user.getId(), deck.getId())).thenReturn(List.of(learning));

        TrainingNextResponse response = service.getNextTrainingCard(user.getId(), deck.getId());

        assertThat(response.finished()).isFalse();
        assertThat(response.card().id()).isEqualTo(card.getId());
        assertThat(response.newCount()).isZero();
        assertThat(response.learningCount()).isEqualTo(1);
        assertThat(response.reviewCount()).isZero();
    }

    @Test
    void reviewCountIncludesOnlyReviewCardsDueToday() {
        User user = user(1L);
        Deck deck = deck(10L, user);
        Flashcard dueCard = card(100L, deck);
        Flashcard futureCard = card(101L, deck);
        FlashcardProgress dueReview = progress(
                1000L,
                user,
                dueCard,
                LearningStatus.REVIEW,
                LocalDate.now().atStartOfDay()
        );
        FlashcardProgress futureReview = progress(
                1001L,
                user,
                futureCard,
                LearningStatus.REVIEW,
                LocalDate.now().plusDays(1).atStartOfDay()
        );

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(flashcardRepository.findNewCardsInDeck(user.getId(), deck.getId())).thenReturn(List.of());
        when(progressRepository.findByUserAndDeck(user.getId(), deck.getId()))
                .thenReturn(List.of(dueReview, futureReview));

        TrainingNextResponse response = service.getNextTrainingCard(user.getId(), deck.getId());

        assertThat(response.finished()).isFalse();
        assertThat(response.card().id()).isEqualTo(dueCard.getId());
        assertThat(response.reviewCount()).isEqualTo(1);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Student");
        user.setEmail("student@example.com");
        return user;
    }

    private Deck deck(Long id, User author) {
        Deck deck = new Deck();
        deck.setId(id);
        deck.setAuthor(author);
        deck.setName("Deck");
        deck.setDescription("Deck description");
        deck.setLevel("A1");
        return deck;
    }

    private Flashcard card(Long id, Deck deck) {
        Flashcard card = new Flashcard();
        card.setId(id);
        card.setDeck(deck);
        card.setEnglishWord("word-" + id);
        card.setTranslation("translation-" + id);
        return card;
    }

    private FlashcardProgress progress(
            Long id,
            User user,
            Flashcard card,
            LearningStatus status,
            LocalDateTime nextReviewAt
    ) {
        FlashcardProgress progress = new FlashcardProgress();
        progress.setId(id);
        progress.setUser(user);
        progress.setFlashcard(card);
        progress.setStatus(status);
        progress.setNextReviewAt(nextReviewAt);
        progress.setNextReviewDate(nextReviewAt.toLocalDate());
        progress.setLastReviewedAt(nextReviewAt.minusMinutes(10));
        return progress;
    }
}
