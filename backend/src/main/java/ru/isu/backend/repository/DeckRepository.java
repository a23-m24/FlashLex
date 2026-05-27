package ru.isu.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.model.Deck;

import java.util.List;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    List<Deck> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    Page<Deck> findByPublishedTrue(Pageable pageable);

    @Query(
            value = """
                    select distinct d
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
