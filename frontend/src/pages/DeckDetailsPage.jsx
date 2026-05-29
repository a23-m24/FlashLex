import { Copy, Download, Edit, Globe2, Play, Star, Trash2 } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useFlashLex } from '../features/auth/useFlashLex'
import {
  difficultyLabels,
  phraseTypeLabels,
  statusLabels,
} from '../shared/lib/labels'
import { cardsToCsv, downloadCsv, safeCsvFilename } from '../shared/lib/csv'
import { getDeckCards, getProgress, getStatusSummary } from '../shared/lib/metrics'
import { Badge } from '../shared/ui/Badge'
import { Button } from '../shared/ui/Button'
import { LinkButton } from '../shared/ui/LinkButton'
import { ProgressBar } from '../shared/ui/ProgressBar'

export function DeckDetailsPage() {
  const { deckId } = useParams()
  const {
    user,
    decks,
    flashcards,
    progress,
    cloneDeck,
    deleteDeck,
    loadDeck,
    publishDeck,
    rateDeck,
    removeDeckRating,
  } = useFlashLex()
  const navigate = useNavigate()
  const [loadError, setLoadError] = useState({ deckId: null, message: '' })
  const deck = decks.find((item) => item.id === deckId)
  const cards = useMemo(
    () => (deck ? getDeckCards(flashcards, deck.id) : []),
    [deck, flashcards],
  )
  const needsFullDeck = Boolean(
    deck && (deck.metrics?.cardCount || 0) > cards.length,
  )
  const activeLoadError = loadError.deckId === deckId ? loadError.message : ''

  useEffect(() => {
    let cancelled = false
    if (!deckId || (deck && !needsFullDeck)) {
      return () => {
        cancelled = true
      }
    }

    loadDeck(deckId)
      .catch((error) => {
        if (!cancelled) {
          setLoadError({ deckId, message: error.message })
        }
      })

    return () => {
      cancelled = true
    }
  }, [deck, deckId, loadDeck, needsFullDeck])

  if (!deck) {
    return (
      <section className="panel centered-panel">
        <h1>{activeLoadError ? 'Набор не найден' : 'Загрузка набора'}</h1>
        {activeLoadError ? <p className="field__error">{activeLoadError}</p> : null}
        <LinkButton to="/">Вернуться к наборам</LinkButton>
      </section>
    )
  }

  const summary = getStatusSummary(cards, progress)
  const isOwner = deck.authorId === user.id
  const ownedClone = decks.find((item) => item.authorId === user.id && item.sourceDeckId === deck.id)
  const total = cards.length || 1
  const ratingLabel = deck.rating ? deck.rating.toFixed(1) : '-'
  const ratingDescription = !deck.ratingTargetId
    ? 'Личный набор без источника.'
    : deck.ratingTargetId === deck.id
      ? 'Рейтинг этого публичного набора.'
      : `Оценка исходного набора: ${deck.sourceDeckName}.`

  const handleClone = async () => {
    const clonedId = await cloneDeck(deck.id)
    if (clonedId) navigate(`/decks/${clonedId}`)
  }

  const handleDelete = async () => {
    if (!window.confirm('Удалить набор вместе с карточками и прогрессом?')) return
    await deleteDeck(deck.id)
    navigate('/')
  }

  const handleRating = async (value) => {
    await rateDeck(deck.id, value)
  }

  const handleRemoveRating = async () => {
    await removeDeckRating(deck.id)
  }

  const handleExportCsv = () => {
    downloadCsv(safeCsvFilename(deck.name), cardsToCsv(cards))
  }

  return (
    <div className="page-stack">
      <section className="page-hero">
        <div>
          <span className="eyebrow">{deck.tags?.[0] || deck.level}</span>
          <h1>{deck.name}</h1>
          <p>{deck.description}</p>
          <div className="tag-row">
            <Badge tone={deck.isPublished ? 'green' : 'neutral'}>
              {deck.isPublished ? 'Публичный' : 'Личный'}
            </Badge>
            <Badge tone="blue">{deck.level}</Badge>
            <Badge tone="neutral">{deck.authorName}</Badge>
            {deck.sourceDeckId && !deck.isPublished ? (
              <Badge tone="blue">Из каталога: {deck.sourceDeckName}</Badge>
            ) : null}
            {deck.sourceDeckId && deck.isPublished ? (
              <Badge tone="blue">Основано на: {deck.sourceDeckName}</Badge>
            ) : null}
            {deck.tags?.map((tag) => (
              <Badge key={tag} tone="green">
                {tag}
              </Badge>
            ))}
          </div>
        </div>
        <div className="hero-actions">
          <LinkButton icon={Play} to={`/training?deck=${deck.id}`}>
            Учить
          </LinkButton>
          <Button icon={Download} onClick={handleExportCsv} variant="secondary">
            CSV
          </Button>
          {isOwner ? (
            <>
              <LinkButton icon={Edit} to={`/decks/${deck.id}/edit`} variant="secondary">
                Изменить
              </LinkButton>
              {!deck.isPublished && !user.publicationBanned ? (
                <Button icon={Globe2} onClick={() => publishDeck(deck.id)} variant="ghost">
                  Опубликовать
                </Button>
              ) : null}
              <Button icon={Trash2} onClick={handleDelete} variant="danger">
                Удалить
              </Button>
            </>
          ) : ownedClone ? (
            <LinkButton to={`/decks/${ownedClone.id}`} variant="secondary">
              Открыть мой набор
            </LinkButton>
          ) : (
            <Button icon={Copy} onClick={handleClone} variant="secondary">
              Добавить себе
            </Button>
          )}
        </div>
      </section>

      <div className="dashboard-grid">
        <section className="panel">
          <div className="section-heading">
            <div>
              <h2>Прогресс набора</h2>
              <p>Новые, изучаемые и повторяемые карточки.</p>
            </div>
          </div>
          <ProgressBar label={statusLabels.NEW} max={total} value={summary.NEW} />
          <ProgressBar label={statusLabels.LEARNING} max={total} value={summary.LEARNING} />
          <ProgressBar label={statusLabels.REVIEW} max={total} value={summary.REVIEW} />
        </section>

        <section className="panel">
          <div className="section-heading">
            <div>
              <h2>Состав</h2>
              <p>Теги и типы выражений.</p>
            </div>
          </div>
          <div className="stats-grid stats-grid--compact">
            <div className="mini-stat">
              <strong>{cards.length}</strong>
              <span>карточек</span>
            </div>
            <div className="mini-stat">
              <strong>{ratingLabel}</strong>
              <span>рейтинг ({deck.ratingsCount})</span>
            </div>
            <div className="mini-stat">
              <strong>{deck.clones}</strong>
              <span>добавлений</span>
            </div>
          </div>
        </section>

        <section className="panel">
          <div className="section-heading">
            <div>
              <h2>Оценка</h2>
              <p>{ratingDescription}</p>
            </div>
          </div>
          {deck.canRate ? (
            <div className="rating-control">
              <div className="rating-control__stars" aria-label="Оценка набора">
                {[1, 2, 3, 4, 5].map((value) => (
                  <button
                    aria-label={`Оценить на ${value}`}
                    className={value <= (deck.userRating || 0) ? 'rating-star rating-star--active' : 'rating-star'}
                    key={value}
                    onClick={() => handleRating(value)}
                    type="button"
                  >
                    <Star aria-hidden="true" size={22} />
                  </button>
                ))}
              </div>
              <span>{deck.userRating ? `Ваша оценка: ${deck.userRating}` : 'Вы еще не оценивали'}</span>
              {deck.userRating ? (
                <Button onClick={handleRemoveRating} size="sm" variant="ghost">
                  Убрать оценку
                </Button>
              ) : null}
            </div>
          ) : (
            <p className="muted">
              {deck.ratingTargetId
                ? 'Автор не может оценивать свой набор.'
                : 'Личный набор без источника нельзя оценить.'}
            </p>
          )}
        </section>
      </div>

      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Карточки</h2>
            <p>Слова, устойчивые обороты, транскрипция и пример.</p>
          </div>
        </div>
        <div className="card-table">
          {cards.map((card) => {
            const cardProgress = getProgress(progress, card.id)
            return (
              <article className="word-row" key={card.id}>
                <div>
                  <strong>{card.englishWord}</strong>
                  <span>{card.transcription}</span>
                </div>
                <div>
                  <strong>{card.translation}</strong>
                  <span>{card.exampleSentence}</span>
                </div>
                <div className="tag-row">
                  <Badge tone="blue">{phraseTypeLabels[card.phraseType]}</Badge>
                  <Badge tone={card.difficulty === 'hard' ? 'red' : 'neutral'}>
                    {difficultyLabels[card.difficulty]}
                  </Badge>
                  <Badge tone="green">
                    {statusLabels[cardProgress?.status || 'NEW']}
                  </Badge>
                </div>
              </article>
            )
          })}
        </div>
      </section>
    </div>
  )
}
