package ru.isu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.model.Flashcard;

import java.util.List;
import java.util.Optional;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findByDeckIdOrderByIdAsc(Long deckId);

    @Query("""
            select c
            from Flashcard c
            join fetch c.deck d
            where d.id in :deckIds
            order by d.id asc, c.id asc
            """)
    List<Flashcard> findByDeckIdInOrderByDeckIdAscIdAsc(@Param("deckIds") List<Long> deckIds);

    @Query("""
            select c
            from Flashcard c
            join fetch c.deck d
            join fetch d.author
            where c.id = :flashcardId
            """)
    Optional<Flashcard> findWithDeckAuthorById(@Param("flashcardId") Long flashcardId);

    @Query("""
            select count(c)
            from Flashcard c
            where c.deck.id = :deckId
              and c.deck.author.id = :userId
              and not exists (
                  select p.id
                  from FlashcardProgress p
                  where p.user.id = :userId
                    and p.flashcard.id = c.id
              )
            """)
    long countNewCardsInDeck(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId
    );

    @Query("""
            select c
            from Flashcard c
            where c.deck.id = :deckId
              and c.deck.author.id = :userId
              and not exists (
                  select p.id
                  from FlashcardProgress p
                  where p.user.id = :userId
                    and p.flashcard.id = c.id
              )
            order by c.id asc
            limit 1
            """)
    Optional<Flashcard> findFirstNewCardInDeck(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId
    );

    long countByDeckId(Long deckId);

    @Modifying
    @Query("delete from Flashcard c where c.id in :cardIds")
    void deleteByIdIn(@Param("cardIds") List<Long> cardIds);

    @Modifying
    @Query("delete from Flashcard c where c.deck.id = :deckId")
    void deleteByDeckId(Long deckId);
}
