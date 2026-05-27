import { BarChart3, Flame, Plus, Target, Trophy } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { DeckCard } from '../features/decks/DeckCard'
import { DeckFilters } from '../features/decks/DeckFilters'
import { useFlashLex } from '../features/auth/useFlashLex'
import { DailyGoalWidget } from '../features/goals/DailyGoalWidget'
import { getDueCards } from '../shared/lib/metrics'
import { EmptyState } from '../shared/ui/EmptyState'
import { LinkButton } from '../shared/ui/LinkButton'
import { StatTile } from '../shared/ui/StatTile'

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
    progress,
    dailyStats,
    leaderboard,
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
  const myDeckIds = myDecks.map((deck) => deck.id)
  const myCards = flashcards.filter((card) => myDeckIds.includes(card.deckId))
  const dueCards = getDueCards(myCards, progress)
  const place =
    [...leaderboard].sort((a, b) => b.points - a.points).findIndex((row) => row.userId === user.id) +
    1
  const tags = [...new Set(myDecks.flatMap((deck) => deck.tags || []))]
  const levels = [...new Set(myDecks.map((deck) => deck.level))]

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
          const aCards = flashcards.filter((card) => card.deckId === a.id).length
          const bCards = flashcards.filter((card) => card.deckId === b.id).length
          return bCards - aCards
        }
        return new Date(b.createdAt) - new Date(a.createdAt)
      })
  }, [filters, flashcards, myDecks])

  const handleClone = async (deckId) => {
    const clonedId = await cloneDeck(deckId)
    if (clonedId) navigate(`/decks/${clonedId}`)
  }

  return (
    <div className="page-stack">
      <section className="page-hero">
        <div>
          <span className="eyebrow">Обзор</span>
          <h1>Мои наборы и цель на день</h1>
          <p>Короткая сводка прогресса и библиотека ваших учебных наборов.</p>
        </div>
        <LinkButton icon={Plus} to="/decks/new">
          Создать набор
        </LinkButton>
      </section>

      <DailyGoalWidget
        learned={dailyStats.learned}
        newLimit={user.dailyNewLimit}
        reviewed={dailyStats.reviewed}
        reviewLimit={user.dailyReviewLimit}
      />

      <div className="stats-grid">
        <StatTile
          caption="к повторению"
          icon={Target}
          label="Очередь"
          tone="blue"
          value={dueCards.length}
        />
        <StatTile
          caption="дней подряд"
          icon={Flame}
          label="Серия"
          tone="amber"
          value={dailyStats.streakDays}
        />
        <StatTile
          caption="за сегодня"
          icon={BarChart3}
          label="Очки"
          tone="green"
          value={dailyStats.points}
        />
        <StatTile
          caption="в рейтинге"
          icon={Trophy}
          label="Место"
          tone="red"
          value={`#${place}`}
        />
      </div>

      <section className="section-block">
        <div className="section-heading">
          <div>
            <h2>Мои наборы</h2>
            <p>Личные и опубликованные наборы для тренировки.</p>
          </div>
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
