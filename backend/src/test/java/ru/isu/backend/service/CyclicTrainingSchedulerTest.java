package ru.isu.backend.service;

import org.junit.jupiter.api.Test;
import ru.isu.backend.model.AnswerQuality;
import ru.isu.backend.model.FlashcardProgress;
import ru.isu.backend.model.LearningStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CyclicTrainingSchedulerTest {

    private final CyclicTrainingScheduler scheduler = new CyclicTrainingScheduler();
    private final LocalDateTime now = LocalDateTime.of(2026, 5, 27, 12, 0);

    @Test
    void newCardUsesInitialButtonIntervals() {
        FlashcardProgress again = progress();
        FlashcardProgress hard = progress();
        FlashcardProgress good = progress();
        FlashcardProgress easy = progress();

        scheduler.answer(again, AnswerQuality.AGAIN_0, now);
        scheduler.answer(hard, AnswerQuality.HARD_3, now);
        scheduler.answer(good, AnswerQuality.GOOD_4, now);
        scheduler.answer(easy, AnswerQuality.EASY_5, now);

        assertThat(again.getStatus()).isEqualTo(LearningStatus.LEARNING);
        assertThat(again.getIntervalMinutes()).isEqualTo(10);
        assertThat(hard.getStatus()).isEqualTo(LearningStatus.REVIEW);
        assertThat(hard.getIntervalDays()).isEqualTo(1);
        assertThat(good.getStatus()).isEqualTo(LearningStatus.REVIEW);
        assertThat(good.getIntervalDays()).isEqualTo(3);
        assertThat(easy.getStatus()).isEqualTo(LearningStatus.REVIEW);
        assertThat(easy.getIntervalDays()).isEqualTo(4);
    }

    @Test
    void goodOnFinalLearningStepAndEasyMoveCardToReviewBuffer() {
        FlashcardProgress good = progress();
        good.setStatus(LearningStatus.LEARNING);
        good.setRemainingSteps(1);
        good.setCorrectAnswers(1);
        FlashcardProgress easy = progress();
        easy.setStatus(LearningStatus.LEARNING);
        easy.setCorrectAnswers(1);

        scheduler.answer(good, AnswerQuality.GOOD_4, now);
        scheduler.answer(easy, AnswerQuality.EASY_5, now);

        assertThat(good.getStatus()).isEqualTo(LearningStatus.REVIEW);
        assertThat(good.getIntervalDays()).isEqualTo(1);
        assertThat(easy.getStatus()).isEqualTo(LearningStatus.REVIEW);
        assertThat(easy.getIntervalDays()).isEqualTo(4);
    }

    @Test
    void tenMinuteLearningCardGraduatesEvenWithoutExplicitStep() {
        FlashcardProgress good = progress();
        good.setStatus(LearningStatus.LEARNING);
        good.setIntervalMinutes(10);
        good.setRemainingSteps(0);

        scheduler.answer(good, AnswerQuality.GOOD_4, now);

        assertThat(good.getStatus()).isEqualTo(LearningStatus.REVIEW);
        assertThat(good.getIntervalDays()).isEqualTo(1);
    }

    @Test
    void reviewIntervalsGrowFromPreviousIntervalAndEaseFactor() {
        FlashcardProgress hard = reviewProgress(3, 2.5);
        FlashcardProgress good = reviewProgress(3, 2.5);
        FlashcardProgress easy = reviewProgress(3, 2.5);

        scheduler.answer(hard, AnswerQuality.HARD_3, now);
        scheduler.answer(good, AnswerQuality.GOOD_4, now);
        scheduler.answer(easy, AnswerQuality.EASY_5, now);

        assertThat(hard.getStatus()).isEqualTo(LearningStatus.REVIEW);
        assertThat(hard.getIntervalDays()).isEqualTo(4);
        assertThat(good.getIntervalDays()).isEqualTo(8);
        assertThat(easy.getIntervalDays()).isEqualTo(10);
        assertThat(hard.getEaseFactor()).isEqualTo(2.35);
        assertThat(good.getEaseFactor()).isEqualTo(2.5);
        assertThat(easy.getEaseFactor()).isEqualTo(2.65);
    }

    @Test
    void againOnReviewCardMovesItToRelearning() {
        FlashcardProgress review = reviewProgress(8, 2.5);

        scheduler.answer(review, AnswerQuality.AGAIN_0, now);

        assertThat(review.getStatus()).isEqualTo(LearningStatus.LEARNING);
        assertThat(review.getIntervalMinutes()).isEqualTo(10);
        assertThat(review.getIntervalDays()).isZero();
        assertThat(review.getLapseCount()).isEqualTo(1);
    }

    @Test
    void againOnRelearningCardShortensTheNextInterval() {
        FlashcardProgress relearning = progress();
        relearning.setStatus(LearningStatus.LEARNING);
        relearning.setIntervalMinutes(10);
        relearning.setIntervalDays(0);
        relearning.setLapseCount(1);

        scheduler.answer(relearning, AnswerQuality.AGAIN_0, now);

        assertThat(relearning.getStatus()).isEqualTo(LearningStatus.LEARNING);
        assertThat(relearning.getIntervalMinutes()).isEqualTo(1);
        assertThat(relearning.getIntervalDays()).isZero();
    }

    private FlashcardProgress progress() {
        FlashcardProgress progress = new FlashcardProgress();
        progress.setStatus(LearningStatus.NEW);
        return progress;
    }

    private FlashcardProgress reviewProgress(int intervalDays, double easeFactor) {
        FlashcardProgress progress = new FlashcardProgress();
        progress.setStatus(LearningStatus.REVIEW);
        progress.setIntervalDays(intervalDays);
        progress.setIntervalMinutes(intervalDays * 24 * 60);
        progress.setEaseFactor(easeFactor);
        return progress;
    }
}
