package ru.isu.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.model.Deck;

import java.util.List;
import java.util.Optional;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    interface TrainingDeckContext {
        Long getId();

        Long getAuthorId();

        Integer getDailyNewLimit();

        Integer getDailyReviewLimit();
    }

    @Query("""
            select d
            from Deck d
            join fetch d.author
            left join fetch d.sourceDeck sourceDeck
            left join fetch sourceDeck.author
            where d.author.id = :authorId
            order by d.createdAt desc
            """)
    List<Deck> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    @EntityGraph(attributePaths = {"author", "sourceDeck", "sourceDeck.author"})
    Optional<Deck> findFirstByAuthorIdAndSourceDeckIdOrderByCreatedAtDesc(Long authorId, Long sourceDeckId);

    @EntityGraph(attributePaths = {"author", "sourceDeck", "sourceDeck.author"})
    @Query("""
            select d
            from Deck d
            where d.id = :deckId
            """)
    Optional<Deck> findWithRelationsById(@Param("deckId") Long deckId);

    @Query("""
            select d.id as id,
                   author.id as authorId,
                   author.dailyNewLimit as dailyNewLimit,
                   author.dailyReviewLimit as dailyReviewLimit
            from Deck d
            join d.author author
            where d.id = :deckId
            """)
    Optional<TrainingDeckContext> findTrainingContextById(@Param("deckId") Long deckId);

    @Modifying
    @Query("""
            update Deck d
            set d.sourceDeck = null
            where d.sourceDeck.id = :sourceDeckId
            """)
    void clearSourceDeckReferences(@Param("sourceDeckId") Long sourceDeckId);

    @Query(
            value = """
                    select distinct d
                    from Deck d
                    join fetch d.author author
                    left join fetch d.sourceDeck sourceDeck
                    left join fetch sourceDeck.author
                    left join d.tags tag
                    where d.published = true
                      and (:query is null
                           or lower(d.name) like lower(concat('%', :query, '%'))
                           or lower(d.description) like lower(concat('%', :query, '%'))
                           or lower(author.name) like lower(concat('%', :query, '%'))
                           or lower(tag) like lower(concat('%', :query, '%')))
                      and (:level is null or d.level = :level)
                      and (:tag is null or tag = :tag)
                    """,
            countQuery = """
                    select count(distinct d)
                    from Deck d
                    left join d.tags tag
                    where d.published = true
                      and (:query is null
                           or lower(d.name) like lower(concat('%', :query, '%'))
                           or lower(d.description) like lower(concat('%', :query, '%'))
                           or lower(d.author.name) like lower(concat('%', :query, '%'))
                           or lower(tag) like lower(concat('%', :query, '%')))
                      and (:level is null or d.level = :level)
                      and (:tag is null or tag = :tag)
                    """
    )
    Page<Deck> searchPublished(
            @Param("query") String query,
            @Param("level") String level,
            @Param("tag") String tag,
            Pageable pageable
    );
}
