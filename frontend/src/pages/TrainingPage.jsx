import { ChevronDown, Play } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { TrainingModeSelector } from '../features/training/TrainingModeSelector'
import { useFlashLex } from '../features/auth/useFlashLex'
import { getDeckQueue, groupCardsByDeck, mapProgressByCard } from '../shared/lib/queue'
import { LinkButton } from '../shared/ui/LinkButton'

function DeckOptionContent({ deck, counts }) {
  return (
    <>
      <span className="training-deck-picker__name">{deck.name}</span>
      <span className="training-deck-picker__counts" aria-label="Доступные слова">
        <strong className="training-deck-picker__count training-deck-picker__count--new">
          {counts.newCount}
        </strong>
        <strong className="training-deck-picker__count training-deck-picker__count--learning">
          {counts.learningCount}
        </strong>
        <strong className="training-deck-picker__count training-deck-picker__count--review">
          {counts.reviewCount}
        </strong>
      </span>
    </>
  )
}

export function TrainingPage() {
  const [searchParams] = useSearchParams()
  const { user, decks, flashcards, progress } = useFlashLex()
  const availableDecks = useMemo(
    () => decks.filter((deck) => deck.authorId === user.id),
    [decks, user.id],
  )
  const initialDeckId = searchParams.get('deck') || availableDecks[0]?.id || ''
  const [deckId, setDeckId] = useState(initialDeckId)
  const [isDeckPickerOpen, setIsDeckPickerOpen] = useState(false)
  const [directionMode, setDirectionMode] = useState('DIRECT_TRANSLATION')
  const [answerMode, setAnswerMode] = useState('SELF_CHECK')
  const cardsByDeckId = useMemo(
    () => groupCardsByDeck(flashcards),
    [flashcards],
  )
  const progressByCardId = useMemo(
    () => mapProgressByCard(progress),
    [progress],
  )
  const deckOptions = useMemo(
    () =>
      availableDecks.map((deck) => ({
        deck,
        counts: getDeckQueue(deck.id, cardsByDeckId, progressByCardId),
      })),
    [availableDecks, cardsByDeckId, progressByCardId],
  )
  const selectedOption = deckOptions.find((option) => option.deck.id === deckId) || deckOptions[0]
  const effectiveDeckId = selectedOption?.deck.id || deckId

  const startUrl = `/training/session?deck=${effectiveDeckId}&direction=${directionMode}&answer=${answerMode}`
  const selectDeck = (nextDeckId) => {
    setDeckId(nextDeckId)
    setIsDeckPickerOpen(false)
  }

  return (
    <div className="page-stack">
      <section aria-label="Выбор режима тренировки" className="page-hero training-start-card">
        <h1>Выбор режима тренировки</h1>
        <LinkButton icon={Play} to={startUrl}>
          Начать
        </LinkButton>
      </section>

      <section className="panel">
        <h2 className="training-deck-title">Набор</h2>
        <div className="training-deck-picker">
          <button
            aria-expanded={isDeckPickerOpen}
            className="training-deck-picker__button"
            onClick={() => setIsDeckPickerOpen((current) => !current)}
            type="button"
          >
            {selectedOption ? (
              <DeckOptionContent counts={selectedOption.counts} deck={selectedOption.deck} />
            ) : (
              <span className="training-deck-picker__name">Нет наборов</span>
            )}
            <ChevronDown aria-hidden="true" size={18} />
          </button>
          {isDeckPickerOpen ? (
            <div className="training-deck-picker__menu">
              {deckOptions.map((option) => (
                <button
                  className={
                    option.deck.id === deckId
                      ? 'training-deck-picker__option training-deck-picker__option--active'
                      : 'training-deck-picker__option'
                  }
                  key={option.deck.id}
                  onClick={() => selectDeck(option.deck.id)}
                  type="button"
                >
                  <DeckOptionContent counts={option.counts} deck={option.deck} />
                </button>
              ))}
            </div>
          ) : null}
        </div>
      </section>

      <TrainingModeSelector
        answerMode={answerMode}
        directionMode={directionMode}
        onAnswerModeChange={setAnswerMode}
        onDirectionModeChange={setDirectionMode}
      />
    </div>
  )
}
