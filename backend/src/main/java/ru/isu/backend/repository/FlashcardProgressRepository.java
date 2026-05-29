package ru.isu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.dto.response.ProgressResponse;
import ru.isu.backend.model.AnswerQuality;
import ru.isu.backend.model.Difficulty;
import ru.isu.backend.model.FlashcardProgress;
import ru.isu.backend.model.LearningStatus;
import ru.isu.backend.model.PhraseType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlashcardProgressRepository extends JpaRepository<FlashcardProgress, Long> {

    interface TrainingProgressCardView {
        Long getProgressId();

        Long getUserId();

        Long getFlashcardId();

        LearningStatus getStatus();

        Integer getIntervalDays();

        Integer getIntervalMinutes();

        Long getIntervalSeconds();

        Double getEaseFactor();

        LocalDate getNextReviewDate();

        LocalDateTime getNextReviewAt();

        Integer getCorrectAnswers();

        Integer getWrongAnswers();

        Integer getRemainingSteps();

        Integer getLapseCount();

        Boolean getLeeched();

        LocalDateTime getLastReviewedAt();

        AnswerQuality getLastAnswerQuality();

        String getEnglishWord();

        String getTranslation();

        String getTranscription();

        String getExampleSentence();

        PhraseType getPhraseType();

        Difficulty getDifficulty();
    }

    @Query("""
            select p
            from FlashcardProgress p
            where p.user.id = :userId
              and p.flashcard.id = :flashcardId
            """)
    Optional<FlashcardProgress> findByUserIdAndFlashcardId(
            @Param("userId") Long userId,
            @Param("flashcardId") Long flashcardId
    );

    @Query("""
            select new ru.isu.backend.dto.response.ProgressResponse(
                   p.id,
                   p.user.id,
                   p.flashcard.id,
                   p.status,
                   p.intervalDays,
                   p.intervalMinutes,
                   p.intervalSeconds,
                   p.easeFactor,
                   p.nextReviewDate,
                   p.nextReviewAt,
                   p.correctAnswers,
                   p.wrongAnswers,
                   p.remainingSteps,
                   p.lapseCount,
                   p.leeched,
                   p.lastReviewedAt,
                   p.lastAnswerQuality
            )
            from FlashcardProgress p
            where p.user.id = :userId
            """)
    List<ProgressResponse> findResponsesByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from FlashcardProgress p where p.flashcard.deck.id = :deckId")
    void deleteByFlashcardDeckId(@Param("deckId") Long deckId);

    @Modifying
    @Query("delete from FlashcardProgress p where p.flashcard.id in :flashcardIds")
    void deleteByFlashcardIdIn(@Param("flashcardIds") List<Long> flashcardIds);

    @Query("""
            select p.id as progressId,
                   p.user.id as userId,
                   c.id as flashcardId,
                   p.status as status,
                   p.intervalDays as intervalDays,
                   p.intervalMinutes as intervalMinutes,
                   p.intervalSeconds as intervalSeconds,
                   p.easeFactor as easeFactor,
                   p.nextReviewDate as nextReviewDate,
                   p.nextReviewAt as nextReviewAt,
                   p.correctAnswers as correctAnswers,
                   p.wrongAnswers as wrongAnswers,
                   p.remainingSteps as remainingSteps,
                   p.lapseCount as lapseCount,
                   p.leeched as leeched,
                   p.lastReviewedAt as lastReviewedAt,
                   p.lastAnswerQuality as lastAnswerQuality,
                   c.englishWord as englishWord,
                   c.translation as translation,
                   c.transcription as transcription,
                   c.exampleSentence as exampleSentence,
                   c.phraseType as phraseType,
                   c.difficulty as difficulty
            from FlashcardProgress p
            join p.flashcard c
            where p.user.id = :userId
              and c.deck.id = :deckId
              and p.status = :status
              and p.nextReviewDate <= :today
            order by p.nextReviewAt asc, p.id asc
            limit 1
            """)
    Optional<TrainingProgressCardView> findFirstDueReviewCardByUserAndDeck(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("status") LearningStatus status,
            @Param("today") LocalDate today
    );

    @Query("""
            select p.id as progressId,
                   p.user.id as userId,
                   c.id as flashcardId,
                   p.status as status,
                   p.intervalDays as intervalDays,
                   p.intervalMinutes as intervalMinutes,
                   p.intervalSeconds as intervalSeconds,
                   p.easeFactor as easeFactor,
                   p.nextReviewDate as nextReviewDate,
                   p.nextReviewAt as nextReviewAt,
                   p.correctAnswers as correctAnswers,
                   p.wrongAnswers as wrongAnswers,
                   p.remainingSteps as remainingSteps,
                   p.lapseCount as lapseCount,
                   p.leeched as leeched,
                   p.lastReviewedAt as lastReviewedAt,
                   p.lastAnswerQuality as lastAnswerQuality,
                   c.englishWord as englishWord,
                   c.translation as translation,
                   c.transcription as transcription,
                   c.exampleSentence as exampleSentence,
                   c.phraseType as phraseType,
                   c.difficulty as difficulty
            from FlashcardProgress p
            join p.flashcard c
            where p.user.id = :userId
              and c.deck.id = :deckId
              and p.status = :status
              and (p.nextReviewAt is null or p.nextReviewAt <= :now)
            order by p.nextReviewAt asc, p.id asc
            limit 1
            """)
    Optional<TrainingProgressCardView> findFirstLearningCardByUserAndDeck(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("status") LearningStatus status,
            @Param("now") LocalDateTime now
    );

}
