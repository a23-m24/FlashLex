package ru.isu.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.isu.backend.dto.request.AnswerCardRequest;
import ru.isu.backend.dto.response.DailyStatsResponse;
import ru.isu.backend.dto.response.LeaderboardRowResponse;
import ru.isu.backend.dto.response.ProgressResponse;
import ru.isu.backend.dto.response.TrainingNextResponse;
import ru.isu.backend.exception.ForbiddenOperationException;
import ru.isu.backend.exception.NotFoundException;
import ru.isu.backend.mapper.DeckMapper;
import ru.isu.backend.mapper.ProgressMapper;
import ru.isu.backend.model.AnswerQuality;
import ru.isu.backend.model.DailyUserStats;
import ru.isu.backend.model.Deck;
import ru.isu.backend.model.Flashcard;
import ru.isu.backend.model.FlashcardProgress;
import ru.isu.backend.model.LeaderboardPeriod;
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
        DailyUserStats stats = getOrCreateTodayStats(userId, LocalDate.now());
        return progressMapper.toDailyStatsResponse(stats);
    }

    @Transactional
    public List<DailyStatsResponse> getDailyStatsHistory(Long userId, Integer days) {
        LocalDate today = LocalDate.now();
        int safeDays = Math.max(1, Math.min(31, nullToZero(days)));
        LocalDate startDate = today.minusDays(safeDays - 1L);
        Map<LocalDate, DailyUserStats> statsByDate = new HashMap<>();
        statsRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, startDate, today)
                .forEach(stats -> statsByDate.put(stats.getDate(), stats));

        return startDate.datesUntil(today.plusDays(1))
                .map(date -> {
                    DailyUserStats stats = statsByDate.getOrDefault(date, emptyStats(userId, date));
                    return progressMapper.toDailyStatsResponse(stats);
                })
                .toList();
    }

    public List<LeaderboardRowResponse> getLeaderboard() {
        return getLeaderboard(LeaderboardPeriod.DAY);
    }

    public List<LeaderboardRowResponse> getLeaderboard(LeaderboardPeriod period) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = period == LeaderboardPeriod.WEEK ? today.minusDays(6) : today;
        Map<Long, List<DailyUserStats>> statsByUser = new HashMap<>();
        statsRepository.findByDateBetween(startDate, today).forEach(stats ->
                statsByUser.computeIfAbsent(stats.getUser().getId(), ignored -> new java.util.ArrayList<>()).add(stats)
        );

        return statsByUser.values().stream()
                .map(stats -> toLeaderboardRow(stats.get(0).getUser(), stats, today))
                .sorted(Comparator.comparing(LeaderboardRowResponse::points).reversed())
                .toList();
    }

    public TrainingNextResponse getNextTrainingCard(Long userId, Long deckId) {
        return getNextTrainingCard(userId, deckId, TrainingQueueMode.GOAL);
    }

    public TrainingNextResponse getNextTrainingCard(Long userId, Long deckId, TrainingQueueMode queueMode) {
        return getNextTrainingCard(userId, deckId, queueMode, 0);
    }

    public TrainingNextResponse getNextTrainingCard(
            Long userId,
            Long deckId,
            TrainingQueueMode queueMode,
            Integer extraLimit
    ) {
        int effectiveExtraLimit = Math.max(0, nullToZero(extraLimit));
        return getNextTrainingCard(userId, deckId, queueMode, extraLimit, effectiveExtraLimit, effectiveExtraLimit);
    }

    public TrainingNextResponse getNextTrainingCard(
            Long userId,
            Long deckId,
            TrainingQueueMode queueMode,
            Integer extraLimit,
            Integer extraNewLimit,
            Integer extraReviewLimit
    ) {
        TrainingQueueMode effectiveQueueMode = queueMode == null ? TrainingQueueMode.GOAL : queueMode;
        int effectiveExtraLimit = Math.max(0, nullToZero(extraLimit));
        int effectiveExtraNewLimit = Math.max(0, nullToZero(extraNewLimit));
        int effectiveExtraReviewLimit = Math.max(0, nullToZero(extraReviewLimit));
        DeckRepository.TrainingDeckContext deck = deckRepository.findTrainingContextById(deckId)
                .orElseThrow(() -> new NotFoundException("Deck not found"));
        if (!deck.getAuthorId().equals(userId)) {
            throw new ForbiddenOperationException("Only user's own decks can be trained");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        int newCount = safeLongToInt(flashcardRepository.countNewCardsInDeck(userId, deckId));
        int learningCount = safeLongToInt(progressRepository.countByUserAndDeckAndStatus(
                userId,
                deckId,
                LearningStatus.LEARNING
        ));
        int reviewDueTodayCount = safeLongToInt(progressRepository.countDueReviewByUserAndDeck(
                userId,
                deckId,
                LearningStatus.REVIEW,
                today
        ));
        DailyUserStats todayStats = statsRepository.findByUserIdAndDate(userId, now.toLocalDate()).orElse(null);
        int remainingNewGoal = remainingGoal(deck.getDailyNewLimit(), todayStats == null ? 0 : todayStats.getLearned());
        int remainingReviewGoal = remainingGoal(deck.getDailyReviewLimit(), todayStats == null ? 0 : todayStats.getReviewed());
        int visibleNewCount = visibleNewCount(
                effectiveQueueMode,
                newCount,
                remainingNewGoal,
                effectiveExtraLimit,
                effectiveExtraNewLimit
        );
        int visibleReviewCount = visibleReviewCount(
                effectiveQueueMode,
                reviewDueTodayCount,
                remainingReviewGoal,
                effectiveExtraLimit,
                effectiveExtraReviewLimit
        );

        FlashcardProgress selectedReview = effectiveQueueMode != TrainingQueueMode.EXTRA_NEW && visibleReviewCount > 0
                ? progressRepository.findFirstDueReviewByUserAndDeck(userId, deckId, LearningStatus.REVIEW, today)
                .orElse(null)
                : null;
        if (selectedReview != null) {
            return trainingNextResponse(
                    deckId,
                    selectedReview.getFlashcard(),
                    selectedReview,
                    visibleReviewCount,
                    visibleNewCount,
                    learningCount,
                    visibleReviewCount,
                    newCount,
                    reviewDueTodayCount
            );
        }

        if (effectiveQueueMode != TrainingQueueMode.EXTRA_REVIEW && visibleNewCount > 0 && newCount > 0) {
            Flashcard selectedNew = flashcardRepository.findFirstNewCardInDeck(userId, deckId)
                    .orElse(null);
            if (selectedNew != null) {
                return trainingNextResponse(
                        deckId,
                        selectedNew,
                        null,
                        visibleReviewCount,
                        visibleNewCount,
                        learningCount,
                        visibleReviewCount,
                        newCount,
                        reviewDueTodayCount
                );
            }
        }

        FlashcardProgress selectedLearning = learningCount > 0
                ? progressRepository.findFirstLearningByUserAndDeck(userId, deckId, LearningStatus.LEARNING)
                .orElse(null)
                : null;
        if (selectedLearning != null) {
            return trainingNextResponse(
                    deckId,
                    selectedLearning.getFlashcard(),
                    selectedLearning,
                    visibleReviewCount,
                    visibleNewCount,
                    learningCount,
                    visibleReviewCount,
                    newCount,
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
                newCount,
                reviewDueTodayCount,
                Collections.emptyList()
        );
    }

    @Transactional
    public ProgressResponse answerCard(Long userId, AnswerCardRequest request) {
        Flashcard card = flashcardRepository.findWithDeckAuthorById(request.flashcardId())
                .orElseThrow(() -> new NotFoundException("Flashcard not found"));
        if (!card.getDeck().getAuthor().getId().equals(userId)) {
            throw new ForbiddenOperationException("Only cards from user's decks can be trained");
        }
        User user = card.getDeck().getAuthor();

        FlashcardProgress progress = progressRepository.findByUserIdAndFlashcardId(userId, card.getId())
                .orElseGet(() -> {
                    FlashcardProgress created = new FlashcardProgress();
                    created.setUser(user);
                    created.setFlashcard(card);
                    return created;
                });

        boolean wasNew = progress.getId() == null || progress.getStatus() == LearningStatus.NEW;
        boolean wasDueReview = progress.getId() != null
                && trainingScheduler.isReviewDueToday(progress, LocalDate.now());
        boolean correct = request.quality() != AnswerQuality.AGAIN_0;
        trainingScheduler.answer(progress, request.quality(), LocalDateTime.now());
        progress.setCorrectAnswers(progress.getCorrectAnswers() + (correct ? 1 : 0));
        progress.setWrongAnswers(progress.getWrongAnswers() + (correct ? 0 : 1));
        progress.setLastAnswerQuality(request.quality());

        updateDailyStats(user, correct, wasNew, wasDueReview);
        return progressMapper.toProgressResponse(progressRepository.save(progress));
    }

    private TrainingNextResponse trainingNextResponse(
            Long deckId,
            Flashcard card,
            FlashcardProgress progress,
            int dueNowCount,
            int newCount,
            int learningCount,
            int reviewCount,
            int newBufferCount,
            int reviewBufferCount
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
                newBufferCount,
                reviewBufferCount,
                trainingScheduler.answerOptions(progress)
        );
    }

    private int remainingGoal(Integer limit, Integer completed) {
        return Math.max(0, nullToZero(limit) - nullToZero(completed));
    }

    private int visibleCount(int bufferCount, int remainingGoal, boolean ignoreGoal, int extraLimit) {
        if (!ignoreGoal) {
            return Math.min(bufferCount, remainingGoal);
        }
        return extraLimit > 0 ? Math.min(bufferCount, extraLimit) : bufferCount;
    }

    private int visibleNewCount(
            TrainingQueueMode queueMode,
            int bufferCount,
            int remainingGoal,
            int legacyExtraLimit,
            int extraNewLimit
    ) {
        return switch (queueMode) {
            case EXTRA_REVIEW -> 0;
            case EXTRA_MIXED -> Math.min(bufferCount, extraNewLimit);
            case EXTRA_NEW -> visibleCount(bufferCount, remainingGoal, true, legacyExtraLimit);
            case GOAL -> visibleCount(bufferCount, remainingGoal, false, 0);
        };
    }

    private int visibleReviewCount(
            TrainingQueueMode queueMode,
            int bufferCount,
            int remainingGoal,
            int legacyExtraLimit,
            int extraReviewLimit
    ) {
        return switch (queueMode) {
            case EXTRA_NEW -> 0;
            case EXTRA_MIXED -> Math.min(bufferCount, extraReviewLimit);
            case EXTRA_REVIEW -> visibleCount(bufferCount, remainingGoal, true, legacyExtraLimit);
            case GOAL -> visibleCount(bufferCount, remainingGoal, false, 0);
        };
    }

    private void updateDailyStats(User user, boolean correct, boolean learned, boolean reviewed) {
        DailyUserStats stats = getOrCreateTodayStats(user, LocalDate.now());
        stats.setReviewed(stats.getReviewed() + (reviewed ? 1 : 0));
        stats.setLearned(stats.getLearned() + (learned ? 1 : 0));
        stats.setCorrect(stats.getCorrect() + (correct && (learned || reviewed) ? 1 : 0));
        stats.setPoints(calculateLeaderboardScore(user, List.of(stats)).points());
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int safeLongToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
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

    private DailyUserStats getOrCreateTodayStats(Long userId, LocalDate today) {
        return statsRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> {
                    DailyUserStats stats = new DailyUserStats();
                    stats.setUser(userRepository.getReferenceById(userId));
                    stats.setDate(today);
                    int streak = statsRepository.findTopByUserIdAndDateBeforeOrderByDateDesc(userId, today)
                            .filter(previous -> previous.getDate().equals(today.minusDays(1)) && previous.getReviewed() > 0)
                            .map(previous -> previous.getStreakDays() + 1)
                            .orElse(0);
                    stats.setStreakDays(streak);
                    return statsRepository.save(stats);
                });
    }

    private LeaderboardRowResponse toLeaderboardRow(User user, List<DailyUserStats> stats, LocalDate today) {
        LeaderboardScore score = calculateLeaderboardScore(user, stats);
        return new LeaderboardRowResponse(
                user.getId(),
                user.getName(),
                score.learned(),
                score.reviewed(),
                score.extraNew(),
                score.extraReview(),
                score.streakDays(),
                score.accuracy(),
                score.points()
        );
    }

    private LeaderboardScore calculateLeaderboardScore(User user, List<DailyUserStats> stats) {
        int learned = 0;
        int reviewed = 0;
        int correct = 0;
        int goalScore = 0;
        int extraScore = 0;
        int extraNew = 0;
        int extraReview = 0;
        int streakDays = 0;

        List<DailyUserStats> sortedStats = stats.stream()
                .sorted(Comparator.comparing(DailyUserStats::getDate))
                .toList();
        for (DailyUserStats item : sortedStats) {
            int dayLearned = nullToZero(item.getLearned());
            int dayReviewed = nullToZero(item.getReviewed());
            int dayExtraNew = Math.max(0, dayLearned - nullToZero(user.getDailyNewLimit()));
            int dayExtraReview = Math.max(0, dayReviewed - nullToZero(user.getDailyReviewLimit()));

            learned += dayLearned;
            reviewed += dayReviewed;
            correct += nullToZero(item.getCorrect());
            extraNew += dayExtraNew;
            extraReview += dayExtraReview;
            goalScore += Math.min(dayLearned, nullToZero(user.getDailyNewLimit())) * 10;
            goalScore += Math.min(dayReviewed, nullToZero(user.getDailyReviewLimit())) * 4;
            extraScore += Math.min(dayExtraNew, 10) * 3;
            extraScore += Math.min(dayExtraReview, 30);
            streakDays = nullToZero(item.getStreakDays());
        }

        int attempts = learned + reviewed;
        int accuracy = attempts == 0 ? 0 : Math.round(correct * 100f / attempts);
        int accuracyBonus = attempts >= 10 ? Math.round((accuracy / 100f) * 30) : 0;
        int streakBonus = Math.min(streakDays, 30) * 2;
        int points = goalScore + extraScore + accuracyBonus + streakBonus;

        return new LeaderboardScore(
                learned,
                reviewed,
                extraNew,
                extraReview,
                streakDays,
                accuracy,
                points
        );
    }

    private DailyUserStats emptyStats(Long userId, LocalDate date) {
        DailyUserStats stats = new DailyUserStats();
        stats.setUser(userRepository.getReferenceById(userId));
        stats.setDate(date);
        return stats;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private record LeaderboardScore(
            int learned,
            int reviewed,
            int extraNew,
            int extraReview,
            int streakDays,
            int accuracy,
            int points
    ) {
    }
}
