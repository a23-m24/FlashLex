package ru.isu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.dto.response.FlashcardResponse;
import ru.isu.backend.model.Flashcard;

import java.util.List;
import java.util.Optional;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    interface CardOwnerView {
        Long getId();

        Long getDeckId();

        Long getAuthorId();

        Integer getDailyNewLimit();

        Integer getDailyReviewLimit();
    }

    interface DeckMetricsView {
        Long getDeckId();

        Long getCardCount();

        Long getWordCount();

        Long getCollocationCount();

        Long getPhrasalVerbCount();

        Long getIdiomCount();

        Long getPhraseCount();

        Long getEasyCount();

        Long getMediumCount();

        Long getHardCount();
    }

    List<Flashcard> findByDeckIdOrderByIdAsc(Long deckId);

    @Query("""
            select c
            from Flashcard c
            where c.deck.id in :deckIds
            order by c.deck.id asc, c.id asc
            """)
    List<Flashcard> findByDeckIdInOrderByDeckIdAscIdAsc(@Param("deckIds") List<Long> deckIds);

    @Query(value = """
            select c.deck_id as deckId,
                   count(c.id) as cardCount,
                   sum(case when c.phrase_type = 'WORD' then 1 else 0 end) as wordCount,
                   sum(case when c.phrase_type = 'COLLOCATION' then 1 else 0 end) as collocationCount,
                   sum(case when c.phrase_type = 'PHRASAL_VERB' then 1 else 0 end) as phrasalVerbCount,
                   sum(case when c.phrase_type = 'IDIOM' then 1 else 0 end) as idiomCount,
                   sum(case when c.phrase_type = 'PHRASE' then 1 else 0 end) as phraseCount,
                   sum(case when c.difficulty = 'EASY' then 1 else 0 end) as easyCount,
                   sum(case when c.difficulty = 'MEDIUM' then 1 else 0 end) as mediumCount,
                   sum(case when c.difficulty = 'HARD' then 1 else 0 end) as hardCount
            from flashcards c
            where c.deck_id in (:deckIds)
            group by c.deck_id
            """, nativeQuery = true)
    List<DeckMetricsView> findMetricsByDeckIdIn(@Param("deckIds") List<Long> deckIds);

    @Query("""
            select c.id as id,
                   d.id as deckId,
                   author.id as authorId,
                   author.dailyNewLimit as dailyNewLimit,
                   author.dailyReviewLimit as dailyReviewLimit
            from Flashcard c
            join c.deck d
            join d.author author
            where c.id = :flashcardId
            """)
    Optional<CardOwnerView> findOwnerById(@Param("flashcardId") Long flashcardId);

    @Query("""
            select new ru.isu.backend.dto.response.FlashcardResponse(
                   c.id,
                   c.englishWord,
                   c.translation,
                   c.transcription,
                   c.exampleSentence,
                   c.phraseType,
                   c.difficulty
            )
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
    Optional<FlashcardResponse> findFirstNewCardResponseInDeck(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId
    );

    @Modifying
    @Query("delete from Flashcard c where c.id in :cardIds")
    void deleteByIdIn(@Param("cardIds") List<Long> cardIds);

    @Modifying
    @Query("delete from Flashcard c where c.deck.id = :deckId")
    void deleteByDeckId(Long deckId);
}
