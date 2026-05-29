package ru.isu.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.model.Deck;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    boolean existsByAuthorIdAndNameIgnoreCase(Long authorId, String name);

    boolean existsByAuthorIdAndNameIgnoreCaseAndIdNot(Long authorId, String name, Long id);

    interface TrainingSessionContext {
        Long getDeckId();

        Long getAuthorId();

        Integer getDailyNewLimit();

        Integer getDailyReviewLimit();

        Long getNewCount();

        Long getLearningCount();

        Long getReviewDueTodayCount();
    }

    interface DeckSummaryView {
        Long getId();

        String getName();

        String getDescription();

        Long getAuthorId();

        String getAuthorName();

        Long getSourceDeckId();

        String getSourceDeckName();

        Boolean getSourceDeckPublished();

        Long getSourceAuthorId();

        String getSourceAuthorName();

        Double getSourceDeckRating();

        Integer getSourceDeckRatingsCount();

        Boolean getPublished();

        String getLevel();

        Double getRating();

        Integer getRatingsCount();

        Integer getClonesCount();

        LocalDateTime getCreatedAt();
    }

    interface DeckTagView {
        Long getDeckId();

        String getTag();
    }

    interface AdminDeckView {
        Long getId();

        String getName();

        String getDescription();

        Long getAuthorId();

        String getAuthorName();

        String getAuthorEmail();

        Boolean getPublished();

        String getLevel();

        Long getCardCount();

        Double getRating();

        Integer getRatingsCount();

        Integer getClonesCount();

        LocalDateTime getCreatedAt();
    }

    @Query("""
            select d.id as id,
                   d.name as name,
                   d.description as description,
                   author.id as authorId,
                   author.name as authorName,
                   sourceDeck.id as sourceDeckId,
                   sourceDeck.name as sourceDeckName,
                   sourceDeck.published as sourceDeckPublished,
                   sourceAuthor.id as sourceAuthorId,
                   sourceAuthor.name as sourceAuthorName,
                   sourceDeck.rating as sourceDeckRating,
                   sourceDeck.ratingsCount as sourceDeckRatingsCount,
                   d.published as published,
                   d.level as level,
                   d.rating as rating,
                   d.ratingsCount as ratingsCount,
                   d.clonesCount as clonesCount,
                   d.createdAt as createdAt
            from Deck d
            join d.author author
            left join d.sourceDeck sourceDeck
            left join sourceDeck.author sourceAuthor
            where author.id = :authorId
            order by d.createdAt desc
            """)
    List<DeckSummaryView> findSummariesByAuthorIdOrderByCreatedAtDesc(@Param("authorId") Long authorId);

    @EntityGraph(attributePaths = {"author", "sourceDeck", "sourceDeck.author", "tags"})
    Optional<Deck> findFirstByAuthorIdAndSourceDeckIdOrderByCreatedAtDesc(Long authorId, Long sourceDeckId);

    @EntityGraph(attributePaths = {"author", "sourceDeck", "sourceDeck.author", "tags"})
    @Query("""
            select d
            from Deck d
            where d.id = :deckId
            """)
    Optional<Deck> findWithRelationsById(@Param("deckId") Long deckId);

    @Query("""
            select d.id as id,
                   d.name as name,
                   d.description as description,
                   author.id as authorId,
                   author.name as authorName,
                   sourceDeck.id as sourceDeckId,
                   sourceDeck.name as sourceDeckName,
                   sourceDeck.published as sourceDeckPublished,
                   sourceAuthor.id as sourceAuthorId,
                   sourceAuthor.name as sourceAuthorName,
                   sourceDeck.rating as sourceDeckRating,
                   sourceDeck.ratingsCount as sourceDeckRatingsCount,
                   d.published as published,
                   d.level as level,
                   d.rating as rating,
                   d.ratingsCount as ratingsCount,
                   d.clonesCount as clonesCount,
                   d.createdAt as createdAt
            from Deck d
            join d.author author
            left join d.sourceDeck sourceDeck
            left join sourceDeck.author sourceAuthor
            where d.id = :deckId
            """)
    Optional<DeckSummaryView> findSummaryById(@Param("deckId") Long deckId);

    @Query(value = """
            select d.id as deckId,
                   d.author_id as authorId,
                   author.daily_new_limit as dailyNewLimit,
                   author.daily_review_limit as dailyReviewLimit,
                   (
                       select count(c.id)
                       from flashcards c
                       where c.deck_id = :deckId
                         and not exists (
                             select 1
                             from flashcard_progress p
                             where p.user_id = :userId
                               and p.flashcard_id = c.id
                         )
                   ) as newCount,
                   (
                       select count(p.id)
                       from flashcard_progress p
                       join flashcards c on c.id = p.flashcard_id
                       where p.user_id = :userId
                         and c.deck_id = :deckId
                         and p.status = :learningStatus
                         and (p.next_review_at is null or p.next_review_at <= :now)
                   ) as learningCount,
                   (
                       select count(p.id)
                       from flashcard_progress p
                       join flashcards c on c.id = p.flashcard_id
                       where p.user_id = :userId
                         and c.deck_id = :deckId
                         and p.status = :reviewStatus
                         and p.next_review_date <= :today
                   ) as reviewDueTodayCount
            from decks d
            join users author on author.id = d.author_id
            where d.id = :deckId
            """, nativeQuery = true)
    Optional<TrainingSessionContext> findTrainingSessionContext(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("learningStatus") String learningStatus,
            @Param("reviewStatus") String reviewStatus,
            @Param("today") java.time.LocalDate today,
            @Param("now") java.time.LocalDateTime now
    );

    @Query("""
            select d.id as deckId,
                   tag as tag
            from Deck d
            join d.tags tag
            where d.id in :deckIds
            """)
    List<DeckTagView> findTagsByDeckIdIn(@Param("deckIds") List<Long> deckIds);

    @Query("""
            select distinct d.level
            from Deck d
            where d.published = true
            order by d.level
            """)
    List<String> findPublishedLevels();

    @Query("""
            select distinct tag
            from Deck d
            join d.tags tag
            where d.published = true
            order by tag
            """)
    List<String> findPublishedTags();

    long countByPublishedTrue();

    long countByPublishedFalse();

    @Modifying
    @Query("""
            update Deck d
            set d.sourceDeck = null
            where d.sourceDeck.id = :sourceDeckId
            """)
    void clearSourceDeckReferences(@Param("sourceDeckId") Long sourceDeckId);

    @Query(
            value = """
                    select distinct d.id as id,
                           d.name as name,
                           d.description as description,
                           author.id as authorId,
                           author.name as authorName,
                           sourceDeck.id as sourceDeckId,
                           sourceDeck.name as sourceDeckName,
                           sourceDeck.published as sourceDeckPublished,
                           sourceAuthor.id as sourceAuthorId,
                           sourceAuthor.name as sourceAuthorName,
                           sourceDeck.rating as sourceDeckRating,
                           sourceDeck.ratingsCount as sourceDeckRatingsCount,
                           d.published as published,
                           d.level as level,
                           d.rating as rating,
                           d.ratingsCount as ratingsCount,
                           d.clonesCount as clonesCount,
                           d.createdAt as createdAt
                    from Deck d
                    join d.author author
                    left join d.sourceDeck sourceDeck
                    left join sourceDeck.author sourceAuthor
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
    Page<DeckSummaryView> searchPublishedSummaries(
            @Param("query") String query,
            @Param("level") String level,
            @Param("tag") String tag,
            Pageable pageable
    );

    @Query(
            value = """
                    select distinct d.id as id,
                           d.name as name,
                           d.description as description,
                           author.id as authorId,
                           author.name as authorName,
                           author.email as authorEmail,
                           d.published as published,
                           d.level as level,
                           count(distinct c.id) as cardCount,
                           d.rating as rating,
                           d.ratingsCount as ratingsCount,
                           d.clonesCount as clonesCount,
                           d.createdAt as createdAt
                    from Deck d
                    join d.author author
                    left join d.tags tag
                    left join Flashcard c on c.deck = d
                    where d.published = true
                      and (:query is null
                           or lower(d.name) like lower(concat('%', :query, '%'))
                           or lower(d.description) like lower(concat('%', :query, '%'))
                           or lower(author.name) like lower(concat('%', :query, '%'))
                           or lower(author.email) like lower(concat('%', :query, '%'))
                           or lower(tag) like lower(concat('%', :query, '%')))
                    group by d.id,
                             d.name,
                             d.description,
                             author.id,
                             author.name,
                             author.email,
                             d.published,
                             d.level,
                             d.rating,
                             d.ratingsCount,
                             d.clonesCount,
                             d.createdAt
                    """,
            countQuery = """
                    select count(distinct d)
                    from Deck d
                    join d.author author
                    left join d.tags tag
                    where d.published = true
                      and (:query is null
                           or lower(d.name) like lower(concat('%', :query, '%'))
                           or lower(d.description) like lower(concat('%', :query, '%'))
                           or lower(author.name) like lower(concat('%', :query, '%'))
                           or lower(author.email) like lower(concat('%', :query, '%'))
                           or lower(tag) like lower(concat('%', :query, '%')))
                    """
    )
    Page<AdminDeckView> searchAdminDecks(
            @Param("query") String query,
            Pageable pageable
    );

}
