package ru.isu.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.isu.backend.dto.request.AnswerCardRequest;
import ru.isu.backend.dto.response.AnswerCardResponse;
import ru.isu.backend.dto.response.DailyStatsResponse;
import ru.isu.backend.dto.response.FlashcardResponse;
import ru.isu.backend.dto.response.LeaderboardRowResponse;
import ru.isu.backend.dto.response.ProgressResponse;
import ru.isu.backend.dto.response.TrainingNextResponse;
import ru.isu.backend.exception.ForbiddenOperationException;
import ru.isu.backend.exception.NotFoundException;
import ru.isu.backend.mapper.ProgressMapper;
import ru.isu.backend.model.AnswerQuality;
import ru.isu.backend.model.DailyUserStats;
import ru.isu.backend.model.FlashcardProgress;
import ru.isu.backend.model.LeaderboardPeriod;
import ru.isu.backend.model.LearningStatus;
import ru.isu.backend.model.TrainingQueueMode;
import ru.isu.backend.repository.DailyUserStatsRepository;
import ru.isu.backend.repository.DailyUserStatsRepository.DailyGoalStatsView;
import ru.isu.backend.repository.DailyUserStatsRepository.LeaderboardStatsView;
import ru.isu.backend.repository.DeckRepository;
import ru.isu.backend.repository.FlashcardProgressRepository;
import ru.isu.backend.repository.FlashcardProgressRepository.TrainingProgressCardView;
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
    private final CyclicTrainingScheduler trainingScheduler;

    public List<ProgressResponse> getProgress(Long userId) {
        return progressRepository.findResponsesByUserId(userId);
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

    public List<LeaderboardRowResponse> getLeaderboard(LeaderboardPeriod period) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = period == LeaderboardPeriod.WEEK ? today.minusDays(6) : today;
        Map<Long, List<LeaderboardStatsView>> statsByUser = new HashMap<>();
        statsRepository.findLeaderboardStatsBetween(startDate, today).forEach(stats ->
                statsByUser.computeIfAbsent(stats.getUserId(), ignored -> new java.util.ArrayList<>()).add(stats)
        );

        return statsByUser.values().stream()
                .map(this::toLeaderboardRow)
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
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        DeckRepository.TrainingSessionContext deck = deckRepository.findTrainingSessionContext(
                        userId,
                        deckId,
                        LearningStatus.LEARNING.name(),
                        LearningStatus.REVIEW.name(),
                        today,
                        now
                )
                .orElseThrow(() -> new NotFoundException("Deck not found"));
        if (!deck.getAuthorId().equals(userId)) {
            throw new ForbiddenOperationException("Only user's own decks can be trained");
        }

        int newCount = safeLongToInt(deck.getNewCount());
        int learningCount = safeLongToInt(deck.getLearningCount());
        int reviewDueTodayCount = safeLongToInt(deck.getReviewDueTodayCount());
        DailyGoalStatsView todayStats = effectiveQueueMode == TrainingQueueMode.GOAL
                ? statsRepository.findGoalStatsByUserIdAndDate(userId, today).orElse(null)
                : null;
        int remainingNewGoal = effectiveQueueMode == TrainingQueueMode.GOAL
                ? remainingGoal(deck.getDailyNewLimit(), todayStats == null ? 0 : todayStats.getLearned())
                : 0;
        int remainingReviewGoal = effectiveQueueMode == TrainingQueueMode.GOAL
                ? remainingGoal(deck.getDailyReviewLimit(), todayStats == null ? 0 : todayStats.getReviewed())
                : 0;
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

        TrainingProgressCardView selectedReview = effectiveQueueMode != TrainingQueueMode.EXTRA_NEW && visibleReviewCount > 0
                ? progressRepository.findFirstDueReviewCardByUserAndDeck(userId, deckId, LearningStatus.REVIEW, today)
                .orElse(null)
                : null;
        if (selectedReview != null) {
            return trainingNextResponse(
                    deckId,
                    cardResponse(selectedReview),
                    progressResponse(selectedReview),
                    progressForScheduling(selectedReview),
                    visibleReviewCount,
                    visibleNewCount,
                    learningCount,
                    visibleReviewCount,
                    newCount,
                    reviewDueTodayCount
            );
        }

        if (effectiveQueueMode != TrainingQueueMode.EXTRA_REVIEW && visibleNewCount > 0 && newCount > 0) {
            FlashcardResponse selectedNew = flashcardRepository.findFirstNewCardResponseInDeck(userId, deckId)
                    .orElse(null);
            if (selectedNew != null) {
                return trainingNextResponse(
                        deckId,
                        selectedNew,
                        null,
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

        TrainingProgressCardView selectedLearning = effectiveQueueMode == TrainingQueueMode.GOAL && learningCount > 0
                ? progressRepository.findFirstLearningCardByUserAndDeck(userId, deckId, LearningStatus.LEARNING, now)
                .orElse(null)
                : null;
        if (selectedLearning != null) {
            return trainingNextResponse(
                    deckId,
                    cardResponse(selectedLearning),
                    progressResponse(selectedLearning),
                    progressForScheduling(selectedLearning),
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
    public AnswerCardResponse answerCard(Long userId, AnswerCardRequest request) {
        FlashcardRepository.CardOwnerView card = flashcardRepository.findOwnerById(request.flashcardId())
                .orElseThrow(() -> new NotFoundException("Flashcard not found"));
        if (!card.getAuthorId().equals(userId)) {
            throw new ForbiddenOperationException("Only cards from user's decks can be trained");
        }

        FlashcardProgress progress = progressRepository.findByUserIdAndFlashcardId(userId, card.getId())
                .orElseGet(() -> {
                    FlashcardProgress created = new FlashcardProgress();
                    created.setUser(userRepository.getReferenceById(userId));
                    created.setFlashcard(flashcardRepository.getReferenceById(card.getId()));
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

        DailyUserStats stats = updateDailyStats(
                userId,
                card.getDailyNewLimit(),
                card.getDailyReviewLimit(),
                correct,
                wasNew,
                wasDueReview
        );
        return new AnswerCardResponse(
                progressMapper.toProgressResponse(progressRepository.save(progress)),
                progressMapper.toDailyStatsResponse(stats)
        );
    }

    private TrainingNextResponse trainingNextResponse(
            Long deckId,
            FlashcardResponse card,
            ProgressResponse progress,
            FlashcardProgress schedulingProgress,
            int dueNowCount,
            int newCount,
            int learningCount,
            int reviewCount,
            int newBufferCount,
            int reviewBufferCount
    ) {
        return new TrainingNextResponse(
                deckId,
                card,
                progress,
                false,
                dueNowCount,
                newCount,
                learningCount,
                reviewCount,
                newBufferCount,
                reviewBufferCount,
                trainingScheduler.answerOptions(schedulingProgress)
        );
    }

    private FlashcardResponse cardResponse(TrainingProgressCardView progress) {
        return new FlashcardResponse(
                progress.getFlashcardId(),
                progress.getEnglishWord(),
                progress.getTranslation(),
                progress.getTranscription(),
                progress.getExampleSentence(),
                progress.getPhraseType(),
                progress.getDifficulty()
        );
    }

    private ProgressResponse progressResponse(TrainingProgressCardView progress) {
        return new ProgressResponse(
                progress.getProgressId(),
                progress.getUserId(),
                progress.getFlashcardId(),
                progress.getStatus(),
                progress.getIntervalDays(),
                progress.getIntervalMinutes(),
                progress.getIntervalSeconds(),
                progress.getEaseFactor(),
                progress.getNextReviewDate(),
                progress.getNextReviewAt(),
                progress.getCorrectAnswers(),
                progress.getWrongAnswers(),
                progress.getRemainingSteps(),
                progress.getLapseCount(),
                progress.getLeeched(),
                progress.getLastReviewedAt(),
                progress.getLastAnswerQuality()
        );
    }

    private FlashcardProgress progressForScheduling(TrainingProgressCardView progress) {
        FlashcardProgress schedulingProgress = new FlashcardProgress();
        schedulingProgress.setId(progress.getProgressId());
        schedulingProgress.setStatus(progress.getStatus());
        schedulingProgress.setIntervalDays(progress.getIntervalDays());
        schedulingProgress.setIntervalMinutes(progress.getIntervalMinutes());
        schedulingProgress.setIntervalSeconds(progress.getIntervalSeconds());
        schedulingProgress.setEaseFactor(progress.getEaseFactor());
        schedulingProgress.setNextReviewDate(progress.getNextReviewDate());
        schedulingProgress.setNextReviewAt(progress.getNextReviewAt());
        schedulingProgress.setCorrectAnswers(progress.getCorrectAnswers());
        schedulingProgress.setWrongAnswers(progress.getWrongAnswers());
        schedulingProgress.setRemainingSteps(progress.getRemainingSteps());
        schedulingProgress.setLapseCount(progress.getLapseCount());
        schedulingProgress.setLeeched(progress.getLeeched());
        schedulingProgress.setLastReviewedAt(progress.getLastReviewedAt());
        schedulingProgress.setLastAnswerQuality(progress.getLastAnswerQuality());
        return schedulingProgress;
    }

    private int remainingGoal(Integer limit, Integer completed) {
        return Math.max(0, nullToZero(limit) - nullToZero(completed));
    }

    private int visibleCount(int bufferCount, int remainingGoal, boolean ignoreGoal, int extraLimit) {
        if (!ignoreGoal) {
            return Math.min(bufferCount, remainingGoal);
        }
        return Math.min(bufferCount, Math.max(0, extraLimit));
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

    private DailyUserStats updateDailyStats(
            Long userId,
            Integer dailyNewLimit,
            Integer dailyReviewLimit,
            boolean correct,
            boolean learned,
            boolean reviewed
    ) {
        DailyUserStats stats = getOrCreateTodayStats(userId, LocalDate.now());
        stats.setReviewed(stats.getReviewed() + (reviewed ? 1 : 0));
        stats.setLearned(stats.getLearned() + (learned ? 1 : 0));
        stats.setCorrect(stats.getCorrect() + (correct && (learned || reviewed) ? 1 : 0));
        stats.setPoints(calculateLeaderboardScore(dailyNewLimit, dailyReviewLimit, List.of(statsEntry(stats))).points());
        return stats;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int safeLongToInt(Long value) {
        if (value == null) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value.intValue();
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

    private LeaderboardRowResponse toLeaderboardRow(List<LeaderboardStatsView> stats) {
        LeaderboardStatsView first = stats.get(0);
        LeaderboardScore score = calculateLeaderboardScore(
                first.getDailyNewLimit(),
                first.getDailyReviewLimit(),
                stats.stream().map(this::statsEntry).toList()
        );
        return new LeaderboardRowResponse(
                first.getUserId(),
                first.getUserName(),
                score.learned(),
                score.reviewed(),
                score.extraNew(),
                score.extraReview(),
                score.streakDays(),
                score.accuracy(),
                score.points()
        );
    }

    private LeaderboardScore calculateLeaderboardScore(
            Integer dailyNewLimit,
            Integer dailyReviewLimit,
            List<StatsEntry> stats
    ) {
        int learned = 0;
        int reviewed = 0;
        int correct = 0;
        int goalScore = 0;
        int extraScore = 0;
        int extraNew = 0;
        int extraReview = 0;
        int streakDays = 0;
        int newGoal = nullToZero(dailyNewLimit);
        int reviewGoal = nullToZero(dailyReviewLimit);

        List<StatsEntry> sortedStats = stats.stream()
                .sorted(Comparator.comparing(StatsEntry::date))
                .toList();
        for (StatsEntry item : sortedStats) {
            int dayLearned = nullToZero(item.learned());
            int dayReviewed = nullToZero(item.reviewed());
            int dayExtraNew = Math.max(0, dayLearned - newGoal);
            int dayExtraReview = Math.max(0, dayReviewed - reviewGoal);

            learned += dayLearned;
            reviewed += dayReviewed;
            correct += nullToZero(item.correct());
            extraNew += dayExtraNew;
            extraReview += dayExtraReview;
            goalScore += Math.min(dayLearned, newGoal) * 10;
            goalScore += Math.min(dayReviewed, reviewGoal) * 4;
            extraScore += Math.min(dayExtraNew, 10) * 3;
            extraScore += Math.min(dayExtraReview, 30);
            streakDays = nullToZero(item.streakDays());
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

    private StatsEntry statsEntry(DailyUserStats stats) {
        return new StatsEntry(
                stats.getDate(),
                stats.getReviewed(),
                stats.getLearned(),
                stats.getCorrect(),
                stats.getStreakDays()
        );
    }

    private StatsEntry statsEntry(LeaderboardStatsView stats) {
        return new StatsEntry(
                stats.getDate(),
                stats.getReviewed(),
                stats.getLearned(),
                stats.getCorrect(),
                stats.getStreakDays()
        );
    }

    private DailyUserStats emptyStats(Long userId, LocalDate date) {
        DailyUserStats stats = new DailyUserStats();
        stats.setUser(userRepository.getReferenceById(userId));
        stats.setDate(date);
        return stats;
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

    private record StatsEntry(
            LocalDate date,
            Integer reviewed,
            Integer learned,
            Integer correct,
            Integer streakDays
    ) {
    }
}
