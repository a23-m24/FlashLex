package ru.isu.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.isu.backend.model.Deck;
import ru.isu.backend.repository.DeckRatingRepository;
import ru.isu.backend.repository.DeckRepository;
import ru.isu.backend.repository.FlashcardProgressRepository;
import ru.isu.backend.repository.FlashcardRepository;

@Service
@RequiredArgsConstructor
public class DeckDeletionService {

    private final DeckRepository deckRepository;
    private final DeckRatingRepository deckRatingRepository;
    private final FlashcardRepository flashcardRepository;
    private final FlashcardProgressRepository flashcardProgressRepository;

    @Transactional
    public void delete(Deck deck) {
        Long deckId = deck.getId();
        deckRatingRepository.deleteByDeckId(deckId);
        deckRepository.clearSourceDeckReferences(deckId);
        flashcardProgressRepository.deleteByFlashcardDeckId(deckId);
        flashcardRepository.deleteByDeckId(deckId);
        deckRepository.delete(deck);
    }
}
