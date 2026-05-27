package ru.isu.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "flashcard_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "flashcard_id"})
)
public class FlashcardProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flashcard_id", nullable = false)
    private Flashcard flashcard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LearningStatus status = LearningStatus.NEW;

    @Column(nullable = false)
    private Integer intervalDays = 0;

    @Column(nullable = false)
    private Integer intervalMinutes = 0;

    @Column(nullable = false)
    private Long intervalSeconds = 0L;

    @Column(nullable = false)
    private Double easeFactor = 2.5;

    @Column(nullable = false)
    private LocalDate nextReviewDate = LocalDate.now();

    @Column
    private LocalDateTime nextReviewAt = LocalDateTime.now();

    @Column(nullable = false)
    private Integer correctAnswers = 0;

    @Column(nullable = false)
    private Integer wrongAnswers = 0;

    @Column(nullable = false)
    private Integer remainingSteps = 0;

    @Column(nullable = false)
    private Integer lapseCount = 0;

    @Column(nullable = false)
    private Boolean leeched = false;

    @Column
    private LocalDateTime lastReviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AnswerQuality lastAnswerQuality;
}
