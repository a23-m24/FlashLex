package ru.isu.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.isu.backend.dto.response.FlashcardResponse;
import ru.isu.backend.dto.response.TrainingNextResponse;
import ru.isu.backend.mapper.ProgressMapper;
import ru.isu.backend.model.Difficulty;
import ru.isu.backend.model.PhraseType;
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

        when(deckRepository.findTrainingSessionContext(
                org.mockito.ArgumentMatchers.eq(user.getId()),
                org.mockito.ArgumentMatchers.eq(deck.getId()),
                org.mockito.ArgumentMatchers.eq(LearningStatus.LEARNING.name()),
                org.mockito.ArgumentMatchers.eq(LearningStatus.REVIEW.name()),
                org.mockito.ArgumentMatchers.eq(LocalDate.now()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(Optional.of(trainingContext(deck, 0L, 1L, 0L)));
        when(progressRepository.findFirstLearningCardByUserAndDeck(
                org.mockito.ArgumentMatchers.eq(user.getId()),
                org.mockito.ArgumentMatchers.eq(deck.getId()),
                org.mockito.ArgumentMatchers.eq(LearningStatus.LEARNING),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        ))
                .thenReturn(Optional.of(trainingProgressView(learning)));

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

        when(deckRepository.findTrainingSessionContext(
                org.mockito.ArgumentMatchers.eq(user.getId()),
                org.mockito.ArgumentMatchers.eq(deck.getId()),
                org.mockito.ArgumentMatchers.eq(LearningStatus.LEARNING.name()),
                org.mockito.ArgumentMatchers.eq(LearningStatus.REVIEW.name()),
                org.mockito.ArgumentMatchers.eq(LocalDate.now()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(Optional.of(trainingContext(deck, 0L, 0L, 1L)));
        when(progressRepository.findFirstDueReviewCardByUserAndDeck(
                user.getId(),
                deck.getId(),
                LearningStatus.REVIEW,
                LocalDate.now()
        )).thenReturn(Optional.of(trainingProgressView(dueReview)));

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
        when(deckRepository.findTrainingSessionContext(
                org.mockito.ArgumentMatchers.eq(user.getId()),
                org.mockito.ArgumentMatchers.eq(deck.getId()),
                org.mockito.ArgumentMatchers.eq(LearningStatus.LEARNING.name()),
                org.mockito.ArgumentMatchers.eq(LearningStatus.REVIEW.name()),
                org.mockito.ArgumentMatchers.eq(LocalDate.now()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(Optional.of(trainingContext(deck, 3L, 0L, 0L)));
        when(flashcardRepository.findFirstNewCardResponseInDeck(user.getId(), deck.getId()))
                .thenReturn(Optional.of(cardResponse(firstCard)));

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
    void extraReviewModeDoesNotExposeAllCardsWhenLimitIsMissing() {
        User user = user(1L);
        Deck deck = deck(10L, user);

        when(deckRepository.findTrainingSessionContext(
                org.mockito.ArgumentMatchers.eq(user.getId()),
                org.mockito.ArgumentMatchers.eq(deck.getId()),
                org.mockito.ArgumentMatchers.eq(LearningStatus.LEARNING.name()),
                org.mockito.ArgumentMatchers.eq(LearningStatus.REVIEW.name()),
                org.mockito.ArgumentMatchers.eq(LocalDate.now()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(Optional.of(trainingContext(deck, 0L, 0L, 5L)));

        TrainingNextResponse response = service.getNextTrainingCard(
                user.getId(),
                deck.getId(),
                TrainingQueueMode.EXTRA_REVIEW,
                0
        );

        assertThat(response.finished()).isTrue();
        assertThat(response.reviewCount()).isZero();
        assertThat(response.reviewBufferCount()).isEqualTo(5);
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

        when(deckRepository.findTrainingSessionContext(
                org.mockito.ArgumentMatchers.eq(user.getId()),
                org.mockito.ArgumentMatchers.eq(deck.getId()),
                org.mockito.ArgumentMatchers.eq(LearningStatus.LEARNING.name()),
                org.mockito.ArgumentMatchers.eq(LearningStatus.REVIEW.name()),
                org.mockito.ArgumentMatchers.eq(LocalDate.now()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(Optional.of(trainingContext(deck, 2L, 0L, 1L)));
        when(flashcardRepository.findFirstNewCardResponseInDeck(user.getId(), deck.getId()))
                .thenReturn(Optional.of(cardResponse(newCard)));

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

    @Test
    void extraReviewModeDoesNotFallBackToLearningCardsWhenLimitIsSpent() {
        User user = user(1L);
        Deck deck = deck(10L, user);

        when(deckRepository.findTrainingSessionContext(
                org.mockito.ArgumentMatchers.eq(user.getId()),
                org.mockito.ArgumentMatchers.eq(deck.getId()),
                org.mockito.ArgumentMatchers.eq(LearningStatus.LEARNING.name()),
                org.mockito.ArgumentMatchers.eq(LearningStatus.REVIEW.name()),
                org.mockito.ArgumentMatchers.eq(LocalDate.now()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(Optional.of(trainingContext(deck, 0L, 4L, 3L)));

        TrainingNextResponse response = service.getNextTrainingCard(
                user.getId(),
                deck.getId(),
                TrainingQueueMode.EXTRA_REVIEW,
                0
        );

        assertThat(response.finished()).isTrue();
        assertThat(response.card()).isNull();
        assertThat(response.learningCount()).isZero();
        assertThat(response.reviewCount()).isZero();
        assertThat(response.reviewBufferCount()).isEqualTo(3);
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

    private DeckRepository.TrainingSessionContext trainingContext(
            Deck deck,
            Long newCount,
            Long learningCount,
            Long reviewDueTodayCount
    ) {
        return new DeckRepository.TrainingSessionContext() {
            @Override
            public Long getDeckId() {
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

            @Override
            public Long getNewCount() {
                return newCount;
            }

            @Override
            public Long getLearningCount() {
                return learningCount;
            }

            @Override
            public Long getReviewDueTodayCount() {
                return reviewDueTodayCount;
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

    private FlashcardResponse cardResponse(Flashcard card) {
        return new FlashcardResponse(
                card.getId(),
                card.getEnglishWord(),
                card.getTranslation(),
                card.getTranscription(),
                card.getExampleSentence(),
                card.getPhraseType(),
                card.getDifficulty()
        );
    }

    private FlashcardProgressRepository.TrainingProgressCardView trainingProgressView(FlashcardProgress progress) {
        return new FlashcardProgressRepository.TrainingProgressCardView() {
            @Override
            public Long getProgressId() {
                return progress.getId();
            }

            @Override
            public Long getUserId() {
                return progress.getUser().getId();
            }

            @Override
            public Long getFlashcardId() {
                return progress.getFlashcard().getId();
            }

            @Override
            public LearningStatus getStatus() {
                return progress.getStatus();
            }

            @Override
            public Integer getIntervalDays() {
                return progress.getIntervalDays();
            }

            @Override
            public Integer getIntervalMinutes() {
                return progress.getIntervalMinutes();
            }

            @Override
            public Long getIntervalSeconds() {
                return progress.getIntervalSeconds();
            }

            @Override
            public Double getEaseFactor() {
                return progress.getEaseFactor();
            }

            @Override
            public LocalDate getNextReviewDate() {
                return progress.getNextReviewDate();
            }

            @Override
            public LocalDateTime getNextReviewAt() {
                return progress.getNextReviewAt();
            }

            @Override
            public Integer getCorrectAnswers() {
                return progress.getCorrectAnswers();
            }

            @Override
            public Integer getWrongAnswers() {
                return progress.getWrongAnswers();
            }

            @Override
            public Integer getRemainingSteps() {
                return progress.getRemainingSteps();
            }

            @Override
            public Integer getLapseCount() {
                return progress.getLapseCount();
            }

            @Override
            public Boolean getLeeched() {
                return progress.getLeeched();
            }

            @Override
            public LocalDateTime getLastReviewedAt() {
                return progress.getLastReviewedAt();
            }

            @Override
            public ru.isu.backend.model.AnswerQuality getLastAnswerQuality() {
                return progress.getLastAnswerQuality();
            }

            @Override
            public String getEnglishWord() {
                return progress.getFlashcard().getEnglishWord();
            }

            @Override
            public String getTranslation() {
                return progress.getFlashcard().getTranslation();
            }

            @Override
            public String getTranscription() {
                return progress.getFlashcard().getTranscription();
            }

            @Override
            public String getExampleSentence() {
                return progress.getFlashcard().getExampleSentence();
            }

            @Override
            public PhraseType getPhraseType() {
                return progress.getFlashcard().getPhraseType();
            }

            @Override
            public Difficulty getDifficulty() {
                return progress.getFlashcard().getDifficulty();
            }
        };
    }
}
