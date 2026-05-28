package ru.isu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.isu.backend.model.DailyUserStats;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyUserStatsRepository extends JpaRepository<DailyUserStats, Long> {

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

    List<DailyUserStats> findByDate(LocalDate date);

    @Query("""
            select s
            from DailyUserStats s
            join fetch s.user
            where s.date between :startDate and :endDate
            """)
    List<DailyUserStats> findByDateBetween(
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
