package ru.isu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.model.DeckRating;

import java.util.List;
import java.util.Optional;

public interface DeckRatingRepository extends JpaRepository<DeckRating, Long> {

    Optional<DeckRating> findByUserIdAndDeckId(Long userId, Long deckId);

    @Query("""
            select r
            from DeckRating r
            join fetch r.deck
            where r.user.id = :userId
              and r.deck.id in :deckIds
            """)
    List<DeckRating> findByUserIdAndDeckIdIn(
            @Param("userId") Long userId,
            @Param("deckIds") List<Long> deckIds
    );

    long countByDeckId(Long deckId);

    @Modifying
    @Query("delete from DeckRating r where r.user.id = :userId and r.deck.id = :deckId")
    void deleteByUserIdAndDeckId(@Param("userId") Long userId, @Param("deckId") Long deckId);

    @Modifying
    @Query("delete from DeckRating r where r.deck.id = :deckId")
    void deleteByDeckId(@Param("deckId") Long deckId);

    @Query("""
            select coalesce(avg(r.value), 0)
            from DeckRating r
            where r.deck.id = :deckId
            """)
    double getAverageRating(@Param("deckId") Long deckId);
}
