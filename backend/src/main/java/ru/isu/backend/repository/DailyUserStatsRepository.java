package ru.isu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.model.DailyUserStats;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyUserStatsRepository extends JpaRepository<DailyUserStats, Long> {

    interface LeaderboardStatsView {
        Long getUserId();

        String getUserName();

        Integer getDailyNewLimit();

        Integer getDailyReviewLimit();

        LocalDate getDate();

        Integer getReviewed();

        Integer getLearned();

        Integer getCorrect();

        Integer getPoints();

        Integer getStreakDays();
    }

    interface DailyGoalStatsView {
        Integer getReviewed();

        Integer getLearned();
    }

    @Query("""
            select s
            from DailyUserStats s
            where s.user.id = :userId
              and s.date = :date
            """)
    Optional<DailyUserStats> findByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    @Query("""
            select s.reviewed as reviewed,
                   s.learned as learned
            from DailyUserStats s
            where s.user.id = :userId
              and s.date = :date
            """)
    Optional<DailyGoalStatsView> findGoalStatsByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    @Query("""
            select s
            from DailyUserStats s
            where s.user.id = :userId
              and s.date < :date
            order by s.date desc
            limit 1
            """)
    Optional<DailyUserStats> findTopByUserIdAndDateBeforeOrderByDateDesc(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    @Query("""
            select s.user.id as userId,
                   s.user.name as userName,
                   s.user.dailyNewLimit as dailyNewLimit,
                   s.user.dailyReviewLimit as dailyReviewLimit,
                   s.date as date,
                   s.reviewed as reviewed,
                   s.learned as learned,
                   s.correct as correct,
                   s.points as points,
                   s.streakDays as streakDays
            from DailyUserStats s
            where s.date between :startDate and :endDate
            """)
    List<LeaderboardStatsView> findLeaderboardStatsBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select s
            from DailyUserStats s
            where s.user.id = :userId
              and s.date between :startDate and :endDate
            order by s.date asc
            """)
    List<DailyUserStats> findByUserIdAndDateBetweenOrderByDateAsc(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
