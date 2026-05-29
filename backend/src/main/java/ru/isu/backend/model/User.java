package ru.isu.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "users",
        indexes = @Index(name = "idx_users_name", columnList = "name")
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'STUDENT'")
    private UserRole role = UserRole.STUDENT;

    @Column(nullable = false)
    private Integer dailyNewLimit = 10;

    @Column(nullable = false)
    private Integer dailyReviewLimit = 25;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean publicationBanned = false;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    void prePersist() {
        if (role == null) {
            role = UserRole.STUDENT;
        }
        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }
        if (publicationBanned == null) {
            publicationBanned = false;
        }
    }
}
