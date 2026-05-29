package ru.isu.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@DynamicUpdate
@Table(
        name = "daily_user_stats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}),
        indexes = {
                @Index(name = "idx_daily_stats_date_points", columnList = "date, points"),
                @Index(name = "idx_daily_stats_user_date", columnList = "user_id, date")
        }
)
public class DailyUserStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer reviewed = 0;

    @Column(nullable = false)
    private Integer learned = 0;

    @Column(nullable = false)
    private Integer correct = 0;

    @Column(nullable = false)
    private Integer points = 0;

    @Column(nullable = false)
    private Integer streakDays = 0;
}
