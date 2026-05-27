package ru.isu.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.isu.backend.dto.request.AnswerCardRequest;
import ru.isu.backend.dto.response.DailyStatsResponse;
import ru.isu.backend.dto.response.LeaderboardRowResponse;
import ru.isu.backend.dto.response.LearningStatsResponse;
import ru.isu.backend.dto.response.ProgressResponse;
import ru.isu.backend.dto.response.TagProgressResponse;
import ru.isu.backend.dto.response.TrainingNextResponse;
import ru.isu.backend.dto.response.WeakCardResponse;
import ru.isu.backend.exception.ForbiddenOperationException;
import ru.isu.backend.exception.NotFoundException;
import ru.isu.backend.mapper.DeckMapper;
import ru.isu.backend.mapper.ProgressMapper;
import ru.isu.backend.model.AnswerQuality;
import ru.isu.backend.model.DailyUserStats;
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
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningService {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final FlashcardRepository flashcardRepository;
    private final FlashcardProgressRepository progressRepository;
    private final DailyUserStatsRepository statsRepository;
    private final ProgressMapper progressMapper;
    private final DeckMapper deckMapper;
    private final CyclicTrainingScheduler trainingScheduler;

    public List<ProgressResponse> getProgress(Long userId) {
        return progressRepository.findByUserId(userId).stream()
                .map(progressMapper::toProgressResponse)
                .toList();
    }

    @Transactional
    public DailyStatsResponse getDailyStats(Long userId) {
        return progressMapper.toDailyStatsResponse(getOrCreateTodayStats(findUser(userId), LocalDate.now()));
    }

    @Transactional
    public LearningStatsResponse getLearningStats(Long userId) {
        User user = findUser(userId);
        LocalDate today = LocalDate.now();
        List<Flashcard> cards = flashcardRepository.findByDeckAuthorIdOrderByIdAsc(userId);
        List<FlashcardProgress> progress = progressRepository.findByUserIdAndFlashcardDeckAuthorId(userId, userId);
        DailyUserStats dailyStats = getOrCreateTodayStats(user, today);

        Map<Long, FlashcardProgress> progressByCardId = new HashMap<>();
        progress.forEach(item -> progressByCardId.put(item.getFlashcard().getId(), item));

        Map<String, Integer> summary = new HashMap<>();
        summary.put(LearningStatus.NEW.name(), 0);
        summary.put(LearningStatus.LEARNING.name(), 0);
        summary.put(LearningStatus.REVIEW.name(), 0);

        int dueToday = 0;
        for (Flashcard card : cards) {
            FlashcardProgress item = progressByCardId.get(card.getId());
            LearningStatus status = item == null ? LearningStatus.NEW : trainingScheduler.normalizeStatus(item.getStatus());
            summary.compute(status.name(), (key, value) -> value == null ? 1 : value + 1);
            if (item == null || trainingScheduler.isAvailableToday(item, LocalDateTime.now())) {
                dueToday++;
            }
        }

        return new LearningStatsResponse(
                cards.size(),
                calculateAccuracy(progress),
                dueToday,
                progressMapper.toDailyStatsResponse(dailyStats),
                summary,
                getWeakCards(userId),
                buildTagProgress(cards, progressByCardId)
        );
    }

    public List<WeakCardResponse> getWeakCards(Long userId) {
        return progressRepository.findWeakProgress(userId, 0.3).stream()
                .filter(item -> item.getFlashcard().getDeck().getAuthor().getId().equals(userId))
                .map(item -> {
                    int answers = item.getCorrectAnswers() + item.getWrongAnswers();
                    double wrongRate = answers == 0 ? 0.0 : item.getWrongAnswers() * 1.0 / answers;
                    return new WeakCardResponse(
                            deckMapper.toFlashcardResponse(item.getFlashcard()),
                            progressMapper.toProgressResponse(item),
                            wrongRate,
                            answers
                    );
                })
                .toList();
    }

    public List<LeaderboardRowResponse> getLeaderboard() {
        LocalDate today = LocalDate.now();
        Map<Long, DailyUserStats> statsByUser = new HashMap<>();
        statsRepository.findByDate(today).forEach(stats -> statsByUser.put(stats.getUser().getId(), stats));

        return userRepository.findAll().stream()
                .map(user -> toLeaderboardRow(user, statsByUser.get(user.getId()), today))
                .sorted(Comparator.comparing(LeaderboardRowResponse::points).reversed())
                .toList();
    }

    public TrainingNextResponse getNextTrainingCard(Long userId, Long deckId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new NotFoundException("Deck not found"));
        if (!deck.getAuthor().getId().equals(userId)) {
            throw new ForbiddenOperationException("Only user's own decks can be trained");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Flashcard> newCards = flashcardRepository.findNewCardsInDeck(userId, deckId);
        List<FlashcardProgress> deckProgress = progressRepository.findByUserAndDeck(userId, deckId);
        List<FlashcardProgress> learningCards = deckProgress.stream()
                .filter(progress -> trainingScheduler.isLearningStatus(progress.getStatus()))
                .toList();
        List<FlashcardProgress> dueReviewCards = deckProgress.stream()
                .filter(progress -> trainingScheduler.isReviewDueToday(progress, now.toLocalDate()))
                .toList();
        int newCount = newCards.size();
        int learningCount = learningCards.size();
        int reviewDueTodayCount = dueReviewCards.size();

        FlashcardProgress selectedReview = selectProgress(dueReviewCards);
        if (selectedReview != null) {
            return trainingNextResponse(
                    deckId,
                    selectedReview.getFlashcard(),
                    selectedReview,
                    reviewDueTodayCount,
                    newCount,
                    learningCount,
                    reviewDueTodayCount
            );
        }

        if (!newCards.isEmpty()) {
            return trainingNextResponse(
                    deckId,
                    newCards.get(0),
                    null,
                    reviewDueTodayCount,
                    newCount,
                    learningCount,
                    reviewDueTodayCount
            );
        }

        FlashcardProgress selectedLearning = selectProgress(learningCards);
        if (selectedLearning != null) {
            return trainingNextResponse(
                    deckId,
                    selectedLearning.getFlashcard(),
                    selectedLearning,
                    reviewDueTodayCount,
                    newCount,
                    learningCount,
                    reviewDueTodayCount
            );
        }

        return new TrainingNextResponse(
                deckId,
                null,
                null,
                true,
                0,
                0,
                0,
                0,
                Collections.emptyList()
        );
    }

    @Transactional
    public ProgressResponse answerCard(Long userId, AnswerCardRequest request) {
        User user = findUser(userId);
        Flashcard card = flashcardRepository.findById(request.flashcardId())
                .orElseThrow(() -> new NotFoundException("Flashcard not found"));
        if (!card.getDeck().getAuthor().getId().equals(userId)) {
            throw new ForbiddenOperationException("Only cards from user's decks can be trained");
        }

        FlashcardProgress progress = progressRepository.findByUserIdAndFlashcardId(userId, card.getId())
                .orElseGet(() -> {
                    FlashcardProgress created = new FlashcardProgress();
                    created.setUser(user);
                    created.setFlashcard(card);
                    return created;
                });

        boolean wasNew = progress.getId() == null || progress.getCorrectAnswers() + progress.getWrongAnswers() == 0;
        boolean correct = request.quality() != AnswerQuality.AGAIN_0;
        trainingScheduler.answer(progress, request.quality(), LocalDateTime.now());
        progress.setCorrectAnswers(progress.getCorrectAnswers() + (correct ? 1 : 0));
        progress.setWrongAnswers(progress.getWrongAnswers() + (correct ? 0 : 1));
        progress.setLastAnswerQuality(request.quality());

        updateDailyStats(user, correct, wasNew, request.quality());
        return progressMapper.toProgressResponse(progressRepository.save(progress));
    }

    private FlashcardProgress selectProgress(List<FlashcardProgress> progress) {
        return progress.stream()
                .min(Comparator.comparing(this::nextReviewAt).thenComparing(FlashcardProgress::getId))
                .orElse(null);
    }

    private TrainingNextResponse trainingNextResponse(
            Long deckId,
            Flashcard card,
            FlashcardProgress progress,
            int dueNowCount,
            int newCount,
            int learningCount,
            int reviewCount
    ) {
        return new TrainingNextResponse(
                deckId,
                deckMapper.toFlashcardResponse(card),
                progress == null ? null : progressMapper.toProgressResponse(progress),
                false,
                dueNowCount,
                newCount,
                learningCount,
                reviewCount,
                trainingScheduler.answerOptions(progress)
        );
    }

    private LocalDateTime nextReviewAt(FlashcardProgress progress) {
        if (progress.getNextReviewAt() != null) {
            return progress.getNextReviewAt();
        }
        return progress.getNextReviewDate().atStartOfDay();
    }

    private void updateDailyStats(User user, boolean correct, boolean learned, AnswerQuality quality) {
        DailyUserStats stats = getOrCreateTodayStats(user, LocalDate.now());
        stats.setReviewed(stats.getReviewed() + 1);
        stats.setLearned(stats.getLearned() + (learned ? 1 : 0));
        stats.setCorrect(stats.getCorrect() + (correct ? 1 : 0));
        stats.setPoints(stats.getPoints() + pointsFor(quality));
    }

    private DailyUserStats getOrCreateTodayStats(User user, LocalDate today) {
        return statsRepository.findByUserIdAndDate(user.getId(), today)
                .orElseGet(() -> {
                    DailyUserStats stats = new DailyUserStats();
                    stats.setUser(user);
                    stats.setDate(today);
                    int streak = statsRepository.findTopByUserIdAndDateBeforeOrderByDateDesc(user.getId(), today)
                            .filter(previous -> previous.getDate().equals(today.minusDays(1)) && previous.getReviewed() > 0)
                            .map(previous -> previous.getStreakDays() + 1)
                            .orElse(0);
                    stats.setStreakDays(streak);
                    return statsRepository.save(stats);
                });
    }

    private LeaderboardRowResponse toLeaderboardRow(User user, DailyUserStats stats, LocalDate today) {
        DailyUserStats effectiveStats = stats == null ? emptyStats(user, today) : stats;
        return new LeaderboardRowResponse(
                user.getId(),
                user.getName(),
                effectiveStats.getLearned(),
                effectiveStats.getStreakDays(),
                accuracy(effectiveStats.getCorrect(), effectiveStats.getReviewed() - effectiveStats.getCorrect()),
                effectiveStats.getPoints()
        );
    }

    private DailyUserStats emptyStats(User user, LocalDate today) {
        DailyUserStats stats = new DailyUserStats();
        stats.setUser(user);
        stats.setDate(today);
        return stats;
    }

    private List<TagProgressResponse> buildTagProgress(
            List<Flashcard> cards,
            Map<Long, FlashcardProgress> progressByCardId
    ) {
        Map<String, int[]> counters = new HashMap<>();
        for (Flashcard card : cards) {
            boolean graduated = progressByCardId.containsKey(card.getId())
                    && trainingScheduler.isReviewStatus(progressByCardId.get(card.getId()).getStatus());
            card.getDeck().getTags().forEach(tag -> {
                int[] values = counters.computeIfAbsent(tag, ignored -> new int[2]);
                values[0]++;
                values[1] += graduated ? 1 : 0;
            });
        }
        return counters.entrySet().stream()
                .map(entry -> {
                    int total = entry.getValue()[0];
                    int graduated = entry.getValue()[1];
                    int percent = total == 0 ? 0 : Math.round(graduated * 100f / total);
                    return new TagProgressResponse(entry.getKey(), total, graduated, percent);
                })
                .sorted(Comparator.comparing(TagProgressResponse::name))
                .toList();
    }

    private int calculateAccuracy(List<FlashcardProgress> progress) {
        int correct = progress.stream().mapToInt(FlashcardProgress::getCorrectAnswers).sum();
        int wrong = progress.stream().mapToInt(FlashcardProgress::getWrongAnswers).sum();
        return accuracy(correct, wrong);
    }

    private int accuracy(int correct, int wrong) {
        int total = correct + wrong;
        return total == 0 ? 0 : Math.round(correct * 100f / total);
    }

    private int pointsFor(AnswerQuality quality) {
        return switch (quality) {
            case EASY_5 -> 18;
            case GOOD_4 -> 14;
            case HARD_3 -> 9;
            case AGAIN_0 -> 3;
        };
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
