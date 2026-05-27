import { Copy, Edit, Globe2, Play, Trash2 } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useFlashLex } from '../features/auth/useFlashLex'
import {
  difficultyLabels,
  phraseTypeLabels,
  statusLabels,
} from '../shared/lib/labels'
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
    publishDeck,
  } = useFlashLex()
  const navigate = useNavigate()
  const deck = decks.find((item) => item.id === deckId)

  if (!deck) {
    return (
      <section className="panel centered-panel">
        <h1>Набор не найден</h1>
        <LinkButton to="/">Вернуться к наборам</LinkButton>
      </section>
    )
  }

  const cards = getDeckCards(flashcards, deck.id)
  const summary = getStatusSummary(cards, progress)
  const isOwner = deck.authorId === user.id
  const total = cards.length || 1

  const handleClone = async () => {
    const clonedId = await cloneDeck(deck.id)
    if (clonedId) navigate(`/decks/${clonedId}`)
  }

  const handleDelete = async () => {
    if (!window.confirm('Удалить набор вместе с карточками и прогрессом?')) return
    await deleteDeck(deck.id)
    navigate('/')
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
          {isOwner ? (
            <>
              <LinkButton icon={Edit} to={`/decks/${deck.id}/edit`} variant="secondary">
                Изменить
              </LinkButton>
              {!deck.isPublished ? (
                <Button icon={Globe2} onClick={() => publishDeck(deck.id)} variant="ghost">
                  Опубликовать
                </Button>
              ) : null}
              <Button icon={Trash2} onClick={handleDelete} variant="danger">
                Удалить
              </Button>
            </>
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
              <strong>{deck.rating || '-'}</strong>
              <span>рейтинг</span>
            </div>
            <div className="mini-stat">
              <strong>{deck.clones}</strong>
              <span>добавлений</span>
            </div>
          </div>
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
