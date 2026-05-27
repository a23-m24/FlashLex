package ru.isu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.model.FlashcardProgress;
import ru.isu.backend.model.LearningStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlashcardProgressRepository extends JpaRepository<FlashcardProgress, Long> {

    Optional<FlashcardProgress> findByUserIdAndFlashcardId(Long userId, Long flashcardId);

    List<FlashcardProgress> findByUserId(Long userId);

    List<FlashcardProgress> findByUserIdAndFlashcardDeckAuthorId(Long userId, Long authorId);

    void deleteByFlashcardDeckId(Long deckId);

    List<FlashcardProgress> findByNextReviewAtIsNull();

    long countByUserIdAndStatus(Long userId, LearningStatus status);

    @Query("""
            select p
            from FlashcardProgress p
            where p.user.id = :userId
              and p.nextReviewDate <= :date
            order by p.nextReviewDate asc
            """)
    List<FlashcardProgress> findDueProgress(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    @Query("""
            select p
            from FlashcardProgress p
            where p.user.id = :userId
              and p.flashcard.deck.id = :deckId
              and (
                   p.nextReviewAt is null
                   or (p.status in :learningStatuses and p.nextReviewAt <= :now)
                   or (p.status in :reviewStatuses and p.nextReviewDate <= :today)
              )
            order by
              case when p.status in :learningStatuses then 0 else 1 end,
              p.nextReviewAt asc,
              p.nextReviewDate asc,
              p.id asc
            """)
    List<FlashcardProgress> findDueProgressInDeck(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("now") LocalDateTime now,
            @Param("today") LocalDate today,
            @Param("learningStatuses") List<LearningStatus> learningStatuses,
            @Param("reviewStatuses") List<LearningStatus> reviewStatuses
    );

    @Query("""
            select p
            from FlashcardProgress p
            where p.user.id = :userId
              and p.flashcard.deck.id = :deckId
            """)
    List<FlashcardProgress> findByUserAndDeck(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId
    );

    @Query("""
            select p
            from FlashcardProgress p
            where p.user.id = :userId
              and (p.correctAnswers + p.wrongAnswers) > 0
              and p.wrongAnswers * 1.0 / (p.correctAnswers + p.wrongAnswers) >= :minWrongRate
            order by (p.wrongAnswers * 1.0 / (p.correctAnswers + p.wrongAnswers)) desc
            """)
    List<FlashcardProgress> findWeakProgress(
            @Param("userId") Long userId,
            @Param("minWrongRate") double minWrongRate
    );
}
