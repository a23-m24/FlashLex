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
import ru.isu.backend.model.TrainingQueueMode;
import ru.isu.backend.model.User;
import ru.isu.backend.repository.DailyUserStatsRepository;
import ru.isu.backend.repository.DeckRepository;
import ru.isu.backend.repository.FlashcardProgressRepository;
import ru.isu.backend.repository.FlashcardRepository;
import ru.isu.backend.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

        when(deckRepository.findTrainingContextById(deck.getId())).thenReturn(Optional.of(trainingContext(deck)));
        when(flashcardRepository.countNewCardsInDeck(user.getId(), deck.getId())).thenReturn(0L);
        when(progressRepository.countByUserAndDeckAndStatus(user.getId(), deck.getId(), LearningStatus.LEARNING))
                .thenReturn(1L);
        when(progressRepository.countDueReviewByUserAndDeck(
                user.getId(),
                deck.getId(),
                LearningStatus.REVIEW,
                LocalDate.now()
        )).thenReturn(0L);
        when(progressRepository.findFirstLearningByUserAndDeck(user.getId(), deck.getId(), LearningStatus.LEARNING))
                .thenReturn(Optional.of(learning));

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

        when(deckRepository.findTrainingContextById(deck.getId())).thenReturn(Optional.of(trainingContext(deck)));
        when(flashcardRepository.countNewCardsInDeck(user.getId(), deck.getId())).thenReturn(0L);
        when(progressRepository.countByUserAndDeckAndStatus(user.getId(), deck.getId(), LearningStatus.LEARNING))
                .thenReturn(0L);
        when(progressRepository.countDueReviewByUserAndDeck(
                user.getId(),
                deck.getId(),
                LearningStatus.REVIEW,
                LocalDate.now()
        )).thenReturn(1L);
        when(progressRepository.findFirstDueReviewByUserAndDeck(
                user.getId(),
                deck.getId(),
                LearningStatus.REVIEW,
                LocalDate.now()
        )).thenReturn(Optional.of(dueReview));

        TrainingNextResponse response = service.getNextTrainingCard(user.getId(), deck.getId());

        assertThat(response.finished()).isFalse();
        assertThat(response.card().id()).isEqualTo(dueCard.getId());
        assertThat(response.reviewCount()).isEqualTo(1);
    }

    @Test
    void extraNewModeCapsVisibleCountByRequestedLimit() {
        User user = user(1L);
        Deck deck = deck(10L, user);
        Flashcard firstCard = card(100L, deck);
        when(deckRepository.findTrainingContextById(deck.getId())).thenReturn(Optional.of(trainingContext(deck)));
        when(flashcardRepository.countNewCardsInDeck(user.getId(), deck.getId())).thenReturn(3L);
        when(progressRepository.countByUserAndDeckAndStatus(user.getId(), deck.getId(), LearningStatus.LEARNING))
                .thenReturn(0L);
        when(progressRepository.countDueReviewByUserAndDeck(
                user.getId(),
                deck.getId(),
                LearningStatus.REVIEW,
                LocalDate.now()
        )).thenReturn(0L);
        when(flashcardRepository.findFirstNewCardInDeck(user.getId(), deck.getId()))
                .thenReturn(Optional.of(firstCard));

        TrainingNextResponse response = service.getNextTrainingCard(
                user.getId(),
                deck.getId(),
                TrainingQueueMode.EXTRA_NEW,
                2
        );

        assertThat(response.finished()).isFalse();
        assertThat(response.card().id()).isEqualTo(firstCard.getId());
        assertThat(response.newCount()).isEqualTo(2);
        assertThat(response.newBufferCount()).isEqualTo(3);
        assertThat(response.reviewCount()).isZero();
    }

    @Test
    void extraMixedModeUsesSeparateNewAndReviewLimits() {
        User user = user(1L);
        Deck deck = deck(10L, user);
        Flashcard newCard = card(100L, deck);
        Flashcard hiddenNewCard = card(101L, deck);
        Flashcard reviewCard = card(102L, deck);
        FlashcardProgress dueReview = progress(
                1000L,
                user,
                reviewCard,
                LearningStatus.REVIEW,
                LocalDate.now().atStartOfDay()
        );

        when(deckRepository.findTrainingContextById(deck.getId())).thenReturn(Optional.of(trainingContext(deck)));
        when(flashcardRepository.countNewCardsInDeck(user.getId(), deck.getId())).thenReturn(2L);
        when(progressRepository.countByUserAndDeckAndStatus(user.getId(), deck.getId(), LearningStatus.LEARNING))
                .thenReturn(0L);
        when(progressRepository.countDueReviewByUserAndDeck(
                user.getId(),
                deck.getId(),
                LearningStatus.REVIEW,
                LocalDate.now()
        )).thenReturn(1L);
        when(flashcardRepository.findFirstNewCardInDeck(user.getId(), deck.getId()))
                .thenReturn(Optional.of(newCard));

        TrainingNextResponse response = service.getNextTrainingCard(
                user.getId(),
                deck.getId(),
                TrainingQueueMode.EXTRA_MIXED,
                0,
                1,
                0
        );

        assertThat(response.finished()).isFalse();
        assertThat(response.card().id()).isEqualTo(newCard.getId());
        assertThat(response.newCount()).isEqualTo(1);
        assertThat(response.reviewCount()).isZero();
        assertThat(response.newBufferCount()).isEqualTo(2);
        assertThat(response.reviewBufferCount()).isEqualTo(1);
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

    private DeckRepository.TrainingDeckContext trainingContext(Deck deck) {
        return new DeckRepository.TrainingDeckContext() {
            @Override
            public Long getId() {
                return deck.getId();
            }

            @Override
            public Long getAuthorId() {
                return deck.getAuthor().getId();
            }

            @Override
            public Integer getDailyNewLimit() {
                return deck.getAuthor().getDailyNewLimit();
            }

            @Override
            public Integer getDailyReviewLimit() {
                return deck.getAuthor().getDailyReviewLimit();
            }
        };
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
