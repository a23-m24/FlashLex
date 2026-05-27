package ru.isu.backend.service;

import org.springframework.stereotype.Component;
import ru.isu.backend.dto.response.TrainingAnswerOptionResponse;
import ru.isu.backend.model.AnswerQuality;
import ru.isu.backend.model.FlashcardProgress;
import ru.isu.backend.model.LearningStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class CyclicTrainingScheduler {

    private static final int DAY_MINUTES = 24 * 60;
    private static final int INITIAL_AGAIN_MINUTES = 10;
    private static final int INITIAL_HARD_DAYS = 1;
    private static final int INITIAL_GOOD_DAYS = 3;
    private static final int INITIAL_EASY_DAYS = 4;
    private static final int FIRST_LEARNING_STEP_MINUTES = 1;
    private static final int SECOND_LEARNING_STEP_MINUTES = 10;
    private static final int HARD_LEARNING_STEP_MINUTES = 6;
    private static final int RELEARNING_STEP_MINUTES = 10;
    private static final int GRADUATING_INTERVAL_DAYS = 1;
    private static final int EASY_INTERVAL_DAYS = 4;
    private static final double STARTING_EASE = 2.5;
    private static final double MINIMUM_EASE = 1.3;
    private static final double HARD_INTERVAL_FACTOR = 1.2;
    private static final double EASY_BONUS = 1.3;

    public void answer(FlashcardProgress progress, AnswerQuality quality, LocalDateTime answeredAt) {
        ScheduleResult schedule = schedule(progress, quality);
        LocalDateTime nextReviewAt = answeredAt.plusMinutes(schedule.intervalMinutes());
        LearningStatus nextStatus = statusForInterval(schedule.intervalMinutes());

        progress.setStatus(nextStatus);
        progress.setIntervalDays(schedule.intervalMinutes() / DAY_MINUTES);
        progress.setIntervalMinutes(schedule.intervalMinutes());
        progress.setIntervalSeconds(schedule.intervalMinutes() * 60L);
        progress.setEaseFactor(schedule.easeFactor());
        progress.setNextReviewAt(nextReviewAt);
        progress.setNextReviewDate(nextReviewAt.toLocalDate());
        progress.setRemainingSteps(schedule.learningStep());
        progress.setLapseCount(schedule.lapseCount());
        progress.setLastReviewedAt(answeredAt);
    }

    public List<TrainingAnswerOptionResponse> answerOptions(FlashcardProgress progress) {
        return List.of(
                toAnswerOption(progress, AnswerQuality.AGAIN_0),
                toAnswerOption(progress, AnswerQuality.HARD_3),
                toAnswerOption(progress, AnswerQuality.GOOD_4),
                toAnswerOption(progress, AnswerQuality.EASY_5)
        );
    }

    public int intervalMinutes(FlashcardProgress progress, AnswerQuality quality) {
        return schedule(progress, quality).intervalMinutes();
    }

    public LearningStatus normalizeStatus(LearningStatus status) {
        if (status == null) {
            return LearningStatus.NEW;
        }
        return status;
    }

    public boolean isLearningStatus(LearningStatus status) {
        return normalizeStatus(status) == LearningStatus.LEARNING;
    }

    public boolean isReviewStatus(LearningStatus status) {
        return normalizeStatus(status) == LearningStatus.REVIEW;
    }

    public boolean isReviewDueToday(FlashcardProgress progress, LocalDate today) {
        return isReviewStatus(progress.getStatus())
                && progress.getNextReviewDate() != null
                && !progress.getNextReviewDate().isAfter(today);
    }

    public boolean isLearningDueNow(FlashcardProgress progress, LocalDateTime now) {
        return isLearningStatus(progress.getStatus())
                && (progress.getNextReviewAt() == null || !progress.getNextReviewAt().isAfter(now));
    }

    public boolean isAvailableToday(FlashcardProgress progress, LocalDateTime now) {
        return isLearningStatus(progress.getStatus()) || isReviewDueToday(progress, now.toLocalDate());
    }

    private TrainingAnswerOptionResponse toAnswerOption(FlashcardProgress progress, AnswerQuality quality) {
        ScheduleResult schedule = schedule(progress, quality);
        return new TrainingAnswerOptionResponse(
                quality,
                schedule.intervalMinutes(),
                schedule.intervalMinutes() / DAY_MINUTES,
                statusForInterval(schedule.intervalMinutes())
        );
    }

    private ScheduleResult schedule(FlashcardProgress progress, AnswerQuality quality) {
        if (isNewCard(progress)) {
            return scheduleNew(quality);
        }
        if (isReviewStatus(progress == null ? null : progress.getStatus())) {
            return scheduleReview(progress, quality);
        }
        if (isRelearning(progress)) {
            return scheduleRelearning(progress, quality);
        }
        return scheduleLearning(progress, quality);
    }

    private ScheduleResult scheduleNew(AnswerQuality quality) {
        return switch (quality) {
            case AGAIN_0 -> learningResult(INITIAL_AGAIN_MINUTES, 0, STARTING_EASE, 0);
            case HARD_3 -> reviewResult(INITIAL_HARD_DAYS, adjustEase(STARTING_EASE, -0.15), 0, 0);
            case GOOD_4 -> reviewResult(INITIAL_GOOD_DAYS, STARTING_EASE, 0, 0);
            case EASY_5 -> reviewResult(INITIAL_EASY_DAYS, adjustEase(STARTING_EASE, 0.15), 0, 0);
        };
    }

    private ScheduleResult scheduleLearning(FlashcardProgress progress, AnswerQuality quality) {
        double easeFactor = normalizedEase(progress);
        int lapseCount = normalizedLapseCount(progress);
        int step = normalizedLearningStep(progress);

        return switch (quality) {
            case AGAIN_0 -> learningResult(FIRST_LEARNING_STEP_MINUTES, 0, easeFactor, lapseCount);
            case HARD_3 -> learningResult(
                    step == 0 ? HARD_LEARNING_STEP_MINUTES : SECOND_LEARNING_STEP_MINUTES,
                    step,
                    adjustEase(easeFactor, -0.15),
                    lapseCount
            );
            case GOOD_4 -> step == 0
                    ? learningResult(SECOND_LEARNING_STEP_MINUTES, 1, easeFactor, lapseCount)
                    : reviewResult(GRADUATING_INTERVAL_DAYS, easeFactor, lapseCount, 0);
            case EASY_5 -> reviewResult(EASY_INTERVAL_DAYS, adjustEase(easeFactor, 0.15), lapseCount, 0);
        };
    }

    private ScheduleResult scheduleRelearning(FlashcardProgress progress, AnswerQuality quality) {
        double easeFactor = normalizedEase(progress);
        int lapseCount = normalizedLapseCount(progress);
        int relearnedInterval = Math.max(GRADUATING_INTERVAL_DAYS, normalizedIntervalDays(progress));

        return switch (quality) {
            case AGAIN_0 -> learningResult(FIRST_LEARNING_STEP_MINUTES, 0, adjustEase(easeFactor, -0.20), lapseCount);
            case HARD_3 -> learningResult(RELEARNING_STEP_MINUTES, 0, adjustEase(easeFactor, -0.15), lapseCount);
            case GOOD_4 -> reviewResult(relearnedInterval, easeFactor, lapseCount, 0);
            case EASY_5 -> reviewResult(Math.max(EASY_INTERVAL_DAYS, relearnedInterval + 1), adjustEase(easeFactor, 0.15), lapseCount, 0);
        };
    }

    private ScheduleResult scheduleReview(FlashcardProgress progress, AnswerQuality quality) {
        double easeFactor = normalizedEase(progress);
        int lapseCount = normalizedLapseCount(progress);
        int currentIntervalDays = normalizedIntervalDays(progress);
        int hardDays = Math.max(currentIntervalDays + 1, (int) Math.ceil(currentIntervalDays * HARD_INTERVAL_FACTOR));
        int goodDays = Math.max(hardDays + 1, (int) Math.round(currentIntervalDays * easeFactor));
        int easyDays = Math.max(goodDays + 1, (int) Math.round(goodDays * EASY_BONUS));

        return switch (quality) {
            case AGAIN_0 -> {
                yield new ScheduleResult(
                        LearningStatus.LEARNING,
                        RELEARNING_STEP_MINUTES,
                        0,
                        adjustEase(easeFactor, -0.20),
                        0,
                        lapseCount + 1
                );
            }
            case HARD_3 -> reviewResult(hardDays, adjustEase(easeFactor, -0.15), lapseCount, 0);
            case GOOD_4 -> reviewResult(goodDays, easeFactor, lapseCount, 0);
            case EASY_5 -> reviewResult(easyDays, adjustEase(easeFactor, 0.15), lapseCount, 0);
        };
    }

    private ScheduleResult learningResult(int intervalMinutes, int step, double easeFactor, int lapseCount) {
        return new ScheduleResult(
                statusForInterval(intervalMinutes),
                intervalMinutes,
                intervalMinutes / DAY_MINUTES,
                easeFactor,
                step,
                lapseCount
        );
    }

    private ScheduleResult reviewResult(int intervalDays, double easeFactor, int lapseCount, int step) {
        return new ScheduleResult(
                LearningStatus.REVIEW,
                intervalDays * DAY_MINUTES,
                intervalDays,
                easeFactor,
                step,
                lapseCount
        );
    }

    private LearningStatus statusForInterval(int intervalMinutes) {
        return intervalMinutes >= DAY_MINUTES ? LearningStatus.REVIEW : LearningStatus.LEARNING;
    }

    private boolean isNewCard(FlashcardProgress progress) {
        if (progress == null) {
            return true;
        }
        return normalizeStatus(progress.getStatus()) == LearningStatus.NEW
                && normalizedAnswerCount(progress) == 0;
    }

    private boolean isRelearning(FlashcardProgress progress) {
        return progress != null
                && isLearningStatus(progress.getStatus())
                && normalizedLapseCount(progress) > 0
                && normalizedIntervalDays(progress) > 0;
    }

    private int normalizedLearningStep(FlashcardProgress progress) {
        if (progress == null) {
            return 0;
        }
        int explicitStep = progress.getRemainingSteps() == null
                ? 0
                : Math.max(0, Math.min(1, progress.getRemainingSteps()));
        if (explicitStep > 0) {
            return explicitStep;
        }
        if (
                isLearningStatus(progress.getStatus())
                        && progress.getIntervalMinutes() != null
                        && progress.getIntervalMinutes() >= SECOND_LEARNING_STEP_MINUTES
        ) {
            return 1;
        }
        return 0;
    }

    private int normalizedIntervalDays(FlashcardProgress progress) {
        if (progress == null) {
            return GRADUATING_INTERVAL_DAYS;
        }
        if (progress.getIntervalMinutes() != null && progress.getIntervalMinutes() > 0) {
            if (progress.getIntervalMinutes() < DAY_MINUTES) {
                return GRADUATING_INTERVAL_DAYS;
            }
            return Math.max(
                    GRADUATING_INTERVAL_DAYS,
                    (int) Math.ceil(progress.getIntervalMinutes() * 1.0 / DAY_MINUTES)
            );
        }
        if (progress.getIntervalDays() != null && progress.getIntervalDays() > 0) {
            return progress.getIntervalDays();
        }
        return GRADUATING_INTERVAL_DAYS;
    }

    private double normalizedEase(FlashcardProgress progress) {
        if (progress == null || progress.getEaseFactor() == null || progress.getEaseFactor() < MINIMUM_EASE) {
            return STARTING_EASE;
        }
        return progress.getEaseFactor();
    }

    private int normalizedLapseCount(FlashcardProgress progress) {
        return progress == null || progress.getLapseCount() == null ? 0 : progress.getLapseCount();
    }

    private int normalizedAnswerCount(FlashcardProgress progress) {
        int correct = progress.getCorrectAnswers() == null ? 0 : progress.getCorrectAnswers();
        int wrong = progress.getWrongAnswers() == null ? 0 : progress.getWrongAnswers();
        return correct + wrong;
    }

    private double adjustEase(double easeFactor, double delta) {
        return Math.max(MINIMUM_EASE, Math.round((easeFactor + delta) * 100.0) / 100.0);
    }

    private record ScheduleResult(
            LearningStatus status,
            int intervalMinutes,
            int intervalDays,
            double easeFactor,
            int learningStep,
            int lapseCount
    ) {
    }
}
