package ru.isu.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.model.User;
import ru.isu.backend.model.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    interface AdminUserView {
        Long getId();

        String getName();

        String getEmail();

        UserRole getRole();

        Integer getDailyNewLimit();

        Integer getDailyReviewLimit();

        Boolean getPublicationBanned();

        LocalDateTime getRegisteredAt();

        Long getDeckCount();

        Long getPublishedDeckCount();

        Long getProgressCount();

        Long getTodayPoints();

        Long getWeekPoints();
    }

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long countByRole(UserRole role);

    long countByPublicationBannedTrue();

    @Query(
            value = """
                    select u.id as id,
                           u.name as name,
                           u.email as email,
                           u.role as role,
                           u.dailyNewLimit as dailyNewLimit,
                           u.dailyReviewLimit as dailyReviewLimit,
                           u.publicationBanned as publicationBanned,
                           u.registeredAt as registeredAt,
                           (select count(d.id) from Deck d where d.author = u) as deckCount,
                           (select count(d.id) from Deck d where d.author = u and d.published = true) as publishedDeckCount,
                           (select count(p.id) from FlashcardProgress p where p.user = u) as progressCount,
                           (select coalesce(sum(s.points), 0) from DailyUserStats s where s.user = u and s.date = :today) as todayPoints,
                           (select coalesce(sum(s.points), 0) from DailyUserStats s where s.user = u and s.date between :weekStart and :today) as weekPoints
                    from User u
                    where (:query is null
                           or lower(u.name) like lower(concat('%', :query, '%'))
                           or lower(u.email) like lower(concat('%', :query, '%')))
                    """,
            countQuery = """
                    select count(u)
                    from User u
                    where (:query is null
                           or lower(u.name) like lower(concat('%', :query, '%'))
                           or lower(u.email) like lower(concat('%', :query, '%')))
                    """
    )
    Page<AdminUserView> searchAdminUsers(
            @Param("query") String query,
            @Param("today") LocalDate today,
            @Param("weekStart") LocalDate weekStart,
            Pageable pageable
    );

    @Query("""
            select u.id as id,
                   u.name as name,
                   u.email as email,
                   u.role as role,
                   u.dailyNewLimit as dailyNewLimit,
                   u.dailyReviewLimit as dailyReviewLimit,
                   u.publicationBanned as publicationBanned,
                   u.registeredAt as registeredAt,
                   (select count(d.id) from Deck d where d.author = u) as deckCount,
                   (select count(d.id) from Deck d where d.author = u and d.published = true) as publishedDeckCount,
                   (select count(p.id) from FlashcardProgress p where p.user = u) as progressCount,
                   (select coalesce(sum(s.points), 0) from DailyUserStats s where s.user = u and s.date = :today) as todayPoints,
                   (select coalesce(sum(s.points), 0) from DailyUserStats s where s.user = u and s.date between :weekStart and :today) as weekPoints
            from User u
            where u.id = :userId
            """)
    Optional<AdminUserView> findAdminUserById(
            @Param("userId") Long userId,
            @Param("today") LocalDate today,
            @Param("weekStart") LocalDate weekStart
    );
}
