package ru.isu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.model.Difficulty;
import ru.isu.backend.model.Flashcard;
import ru.isu.backend.model.PhraseType;

import java.util.List;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findByDeckIdOrderByIdAsc(Long deckId);

    List<Flashcard> findByDeckAuthorIdOrderByIdAsc(Long authorId);

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
            """)
    List<Flashcard> findNewCardsInDeck(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId
    );

    long countByDeckId(Long deckId);

    long countByDeckIdAndPhraseType(Long deckId, PhraseType phraseType);

    long countByDeckIdAndDifficulty(Long deckId, Difficulty difficulty);

    void deleteByDeckId(Long deckId);
}
