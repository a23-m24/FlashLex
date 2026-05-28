import { Copy, Edit, Eye, Play, Star, Users } from 'lucide-react'
import { Link } from 'react-router-dom'
import { phraseTypeLabels } from '../../shared/lib/labels'
import { getDeckCards } from '../../shared/lib/metrics'
import { Badge } from '../../shared/ui/Badge'
import { Button } from '../../shared/ui/Button'
import { LinkButton } from '../../shared/ui/LinkButton'

export function DeckCard({
  deck,
  flashcards,
  currentUserId,
  onClone,
  onPublish,
  ownedCloneId,
  showStudyAction = true,
}) {
  const cards = getDeckCards(flashcards, deck.id)
  const phraseTypes = [...new Set(cards.map((card) => card.phraseType))].slice(0, 3)
  const isOwner = deck.authorId === currentUserId
  const tags = deck.tags || []
  const isPrivateCatalogCopy = deck.sourceDeckId && !deck.isPublished
  const isPublishedDerivative = deck.sourceDeckId && deck.isPublished
  const ratingLabel = deck.rating ? `${deck.rating.toFixed(1)} (${deck.ratingsCount})` : '-'

  return (
    <article className="deck-card">
      <div className="deck-card__head">
        <div>
          <Link className="deck-card__title" to={`/decks/${deck.id}`}>
            {deck.name}
          </Link>
          <p>{deck.description}</p>
        </div>
        <Badge tone={deck.isPublished ? 'green' : isPrivateCatalogCopy ? 'blue' : 'neutral'}>
          {deck.isPublished ? 'Публичный' : isPrivateCatalogCopy ? 'Из каталога' : 'Личный'}
        </Badge>
      </div>

      <div className="deck-card__meta">
        <span>{tags.join(', ')}</span>
        <span>{deck.level}</span>
        <span>{cards.length} карточек</span>
        <span>{deck.authorName}</span>
        {isPrivateCatalogCopy ? <span>Источник: {deck.sourceDeckName}</span> : null}
        {isPublishedDerivative ? <span>Основано на: {deck.sourceDeckName}</span> : null}
      </div>

      <div className="tag-row">
        {tags.slice(0, 3).map((tag) => (
          <Badge key={tag} tone="green">
            {tag}
          </Badge>
        ))}
        {phraseTypes.map((type) => (
          <Badge key={type} tone="blue">
            {phraseTypeLabels[type]}
          </Badge>
        ))}
      </div>

      <div className="deck-card__stats">
        <span>
          <Star aria-hidden="true" size={16} />
          {ratingLabel}
        </span>
        <span>
          <Users aria-hidden="true" size={16} />
          {deck.clones}
        </span>
      </div>

      <div className="deck-card__actions">
        <LinkButton icon={Eye} to={`/decks/${deck.id}`} variant="secondary">
          Открыть
        </LinkButton>
        {isOwner ? (
          <>
            <LinkButton icon={Edit} to={`/decks/${deck.id}/edit`} variant="ghost">
              Изменить
            </LinkButton>
            {!deck.isPublished ? (
              <Button onClick={() => onPublish(deck.id)} variant="ghost">
                Опубликовать
              </Button>
            ) : null}
          </>
        ) : ownedCloneId ? (
          <LinkButton to={`/decks/${ownedCloneId}`} variant="ghost">
            Открыть мой
          </LinkButton>
        ) : (
          <Button icon={Copy} onClick={() => onClone(deck.id)} variant="ghost">
            Добавить
          </Button>
        )}
        {showStudyAction ? (
          <LinkButton icon={Play} to={`/training?deck=${deck.id}`} variant="primary">
            Учить
          </LinkButton>
        ) : null}
      </div>
    </article>
  )
}
