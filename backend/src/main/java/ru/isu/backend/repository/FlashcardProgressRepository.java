package ru.isu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.model.FlashcardProgress;
import ru.isu.backend.model.LearningStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FlashcardProgressRepository extends JpaRepository<FlashcardProgress, Long> {

    @Query("""
            select p
            from FlashcardProgress p
            join fetch p.flashcard
            where p.user.id = :userId
              and p.flashcard.id = :flashcardId
            """)
    Optional<FlashcardProgress> findByUserIdAndFlashcardId(
            @Param("userId") Long userId,
            @Param("flashcardId") Long flashcardId
    );

    @Query("""
            select p
            from FlashcardProgress p
            join fetch p.flashcard
            where p.user.id = :userId
            """)
    List<FlashcardProgress> findByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from FlashcardProgress p where p.flashcard.deck.id = :deckId")
    void deleteByFlashcardDeckId(@Param("deckId") Long deckId);

    @Modifying
    @Query("delete from FlashcardProgress p where p.flashcard.id in :flashcardIds")
    void deleteByFlashcardIdIn(@Param("flashcardIds") List<Long> flashcardIds);

    @Query("""
            select count(p)
            from FlashcardProgress p
            where p.user.id = :userId
              and p.flashcard.deck.id = :deckId
              and p.status = :status
            """)
    long countByUserAndDeckAndStatus(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("status") LearningStatus status
    );

    @Query("""
            select count(p)
            from FlashcardProgress p
            where p.user.id = :userId
              and p.flashcard.deck.id = :deckId
              and p.status = :status
              and p.nextReviewDate <= :today
            """)
    long countDueReviewByUserAndDeck(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("status") LearningStatus status,
            @Param("today") LocalDate today
    );

    @Query("""
            select p
            from FlashcardProgress p
            join fetch p.flashcard
            where p.user.id = :userId
              and p.flashcard.deck.id = :deckId
              and p.status = :status
              and p.nextReviewDate <= :today
            order by p.nextReviewAt asc, p.id asc
            limit 1
            """)
    Optional<FlashcardProgress> findFirstDueReviewByUserAndDeck(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("status") LearningStatus status,
            @Param("today") LocalDate today
    );

    @Query("""
            select p
            from FlashcardProgress p
            join fetch p.flashcard
            where p.user.id = :userId
              and p.flashcard.deck.id = :deckId
              and p.status = :status
            order by p.nextReviewAt asc, p.id asc
            limit 1
            """)
    Optional<FlashcardProgress> findFirstLearningByUserAndDeck(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("status") LearningStatus status
    );

}
