package ru.isu.backend.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "decks",
        indexes = {
                @Index(name = "idx_decks_author_created", columnList = "author_id, created_at"),
                @Index(name = "idx_decks_source", columnList = "source_deck_id"),
                @Index(name = "idx_decks_catalog", columnList = "published, level, rating")
        }
)
public class Deck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_deck_id")
    private Deck sourceDeck;

    @Column(nullable = false)
    private Boolean published = false;

    @Column(nullable = false, length = 10)
    private String level;

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(
            name = "deck_tags",
            joinColumns = @JoinColumn(name = "deck_id"),
            indexes = {
                    @Index(name = "idx_deck_tags_deck", columnList = "deck_id"),
                    @Index(name = "idx_deck_tags_tag", columnList = "tag")
            }
    )
    @Column(name = "tag", nullable = false, length = 50)
    private List<String> tags = new ArrayList<>();

    @Column(nullable = false)
    private Double rating = 0.0;

    @Column(nullable = false)
    private Integer ratingsCount = 0;

    @Column(nullable = false)
    private Integer clonesCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
