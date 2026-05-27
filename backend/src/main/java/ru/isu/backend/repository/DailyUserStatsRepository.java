package ru.isu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.isu.backend.model.DailyUserStats;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyUserStatsRepository extends JpaRepository<DailyUserStats, Long> {

    Optional<DailyUserStats> findByUserIdAndDate(Long userId, LocalDate date);

    Optional<DailyUserStats> findTopByUserIdAndDateBeforeOrderByDateDesc(Long userId, LocalDate date);

    List<DailyUserStats> findByDate(LocalDate date);
}
