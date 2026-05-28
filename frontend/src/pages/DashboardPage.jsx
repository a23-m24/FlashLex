import { Plus } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { DeckCard } from '../features/decks/DeckCard'
import { DeckFilters } from '../features/decks/DeckFilters'
import { useFlashLex } from '../features/auth/useFlashLex'
import { DailyGoalWidget } from '../features/goals/DailyGoalWidget'
import { EmptyState } from '../shared/ui/EmptyState'
import { LinkButton } from '../shared/ui/LinkButton'

const sortOptions = [
  { value: 'created', label: 'Сначала новые' },
  { value: 'name', label: 'По названию' },
  { value: 'cards', label: 'По количеству карточек' },
]

export function DashboardPage() {
  const {
    user,
    decks,
    flashcards,
    dailyStats,
    cloneDeck,
    publishDeck,
  } = useFlashLex()
  const navigate = useNavigate()
  const [filters, setFilters] = useState({
    query: '',
    tag: '',
    level: '',
    sort: 'created',
  })

  const myDecks = useMemo(
    () => decks.filter((deck) => deck.authorId === user.id),
    [decks, user.id],
  )
  const tags = useMemo(
    () => [...new Set(myDecks.flatMap((deck) => deck.tags || []))],
    [myDecks],
  )
  const levels = useMemo(
    () => [...new Set(myDecks.map((deck) => deck.level))],
    [myDecks],
  )
  const cardCountByDeckId = useMemo(
    () =>
      flashcards.reduce((counts, card) => {
        counts.set(card.deckId, (counts.get(card.deckId) || 0) + 1)
        return counts
      }, new Map()),
    [flashcards],
  )

  const filteredDecks = useMemo(() => {
    const query = filters.query.toLowerCase().trim()
    return myDecks
      .filter((deck) => {
        const matchesQuery =
          !query ||
          `${deck.name} ${deck.description} ${(deck.tags || []).join(' ')}`.toLowerCase().includes(query)
        const matchesTag = !filters.tag || deck.tags?.includes(filters.tag)
        const matchesLevel = !filters.level || deck.level === filters.level
        return matchesQuery && matchesTag && matchesLevel
      })
      .sort((a, b) => {
        if (filters.sort === 'name') return a.name.localeCompare(b.name)
        if (filters.sort === 'cards') {
          const aCards = cardCountByDeckId.get(a.id) || 0
          const bCards = cardCountByDeckId.get(b.id) || 0
          return bCards - aCards
        }
        return new Date(b.createdAt) - new Date(a.createdAt)
      })
  }, [cardCountByDeckId, filters, myDecks])

  const handleClone = async (deckId) => {
    const clonedId = await cloneDeck(deckId)
    if (clonedId) navigate(`/decks/${clonedId}`)
  }

  return (
    <div className="page-stack">
      <h1 className="dashboard-title">Главная</h1>

      <DailyGoalWidget
        learned={dailyStats.learned}
        newLimit={user.dailyNewLimit}
        reviewed={dailyStats.reviewed}
        reviewLimit={user.dailyReviewLimit}
      />

      <section className="section-block">
        <div className="section-heading">
          <div>
            <h2>Мои наборы</h2>
            <p>Личные и опубликованные наборы для тренировки.</p>
          </div>
          <LinkButton icon={Plus} to="/decks/new" variant="secondary">
            Создать набор
          </LinkButton>
        </div>
      </section>

      <DeckFilters
        filters={filters}
        levels={levels}
        onChange={setFilters}
        sortOptions={sortOptions}
        tags={tags}
      />

      {filteredDecks.length ? (
        <div className="deck-grid">
          {filteredDecks.map((deck) => (
            <DeckCard
              currentUserId={user.id}
              deck={deck}
              flashcards={flashcards}
              key={deck.id}
              onClone={handleClone}
              onPublish={publishDeck}
            />
          ))}
        </div>
      ) : (
        <EmptyState
          action={<LinkButton to="/decks/new">Создать набор</LinkButton>}
          text="Измените фильтры или добавьте первый набор."
          title="Наборы не найдены"
        />
      )}
    </div>
  )
}
