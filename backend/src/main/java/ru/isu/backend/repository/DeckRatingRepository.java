package ru.isu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.model.DeckRating;

import java.util.Optional;

public interface DeckRatingRepository extends JpaRepository<DeckRating, Long> {

    Optional<DeckRating> findByUserIdAndDeckId(Long userId, Long deckId);

    boolean existsByUserIdAndDeckId(Long userId, Long deckId);

    long countByDeckId(Long deckId);

    void deleteByDeckId(Long deckId);

    @Query("""
            select coalesce(avg(r.value), 0)
            from DeckRating r
            where r.deck.id = :deckId
            """)
    double getAverageRating(@Param("deckId") Long deckId);
}
