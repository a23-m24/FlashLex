import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { DeckCard } from '../features/decks/DeckCard'
import { DeckFilters } from '../features/decks/DeckFilters'
import { useFlashLex } from '../features/auth/useFlashLex'
import { EmptyState } from '../shared/ui/EmptyState'
import { Pagination } from '../shared/ui/Pagination'

const sortOptions = [
  { value: 'rating', label: 'По рейтингу' },
  { value: 'popular', label: 'По добавлениям' },
  { value: 'created', label: 'Сначала новые' },
  { value: 'name', label: 'По названию' },
]

const PAGE_SIZE = 6

export function PublicDecksPage() {
  const { user, decks, flashcards, cloneDeck, publishDeck } = useFlashLex()
  const navigate = useNavigate()
  const [page, setPage] = useState(1)
  const [filters, setFilters] = useState({
    query: '',
    tag: '',
    level: '',
    sort: 'rating',
  })
  const publicDecks = useMemo(
    () => decks.filter((deck) => deck.isPublished),
    [decks],
  )
  const myDecks = useMemo(
    () => decks.filter((deck) => deck.authorId === user.id),
    [decks, user.id],
  )
  const tags = useMemo(
    () => [...new Set(publicDecks.flatMap((deck) => deck.tags || []))],
    [publicDecks],
  )
  const levels = useMemo(
    () => [...new Set(publicDecks.map((deck) => deck.level))],
    [publicDecks],
  )

  const filteredDecks = useMemo(() => {
    const query = filters.query.toLowerCase().trim()
    return publicDecks
      .filter((deck) => {
        const searchValue = `${deck.name} ${deck.description} ${(deck.tags || []).join(' ')} ${deck.authorName}`
        const matchesQuery = !query || searchValue.toLowerCase().includes(query)
        const matchesTag = !filters.tag || deck.tags?.includes(filters.tag)
        const matchesLevel = !filters.level || deck.level === filters.level
        return matchesQuery && matchesTag && matchesLevel
      })
      .sort((a, b) => {
        if (filters.sort === 'popular') return b.clones - a.clones
        if (filters.sort === 'created') return new Date(b.createdAt) - new Date(a.createdAt)
        if (filters.sort === 'name') return a.name.localeCompare(b.name)
        return b.rating - a.rating
      })
  }, [filters, publicDecks])

  const totalPages = Math.max(1, Math.ceil(filteredDecks.length / PAGE_SIZE))
  const visibleDecks = filteredDecks.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  const handleFilters = (nextFilters) => {
    setFilters(nextFilters)
    setPage(1)
  }

  const handleClone = async (deckId) => {
    const clonedId = await cloneDeck(deckId)
    if (clonedId) navigate(`/decks/${clonedId}`)
  }

  return (
    <div className="page-stack">
      <section className="page-hero">
        <div>
          <span className="eyebrow">Каталог</span>
          <h1>Публичные наборы</h1>
          <p>Поиск по названию, тэгам, уровню и автору.</p>
        </div>
      </section>

      <DeckFilters
        filters={filters}
        levels={levels}
        onChange={handleFilters}
        sortOptions={sortOptions}
        tags={tags}
      />

      {visibleDecks.length ? (
        <>
          <div className="deck-grid">
            {visibleDecks.map((deck) => (
              <DeckCard
                currentUserId={user.id}
                deck={deck}
                flashcards={flashcards}
                key={deck.id}
                onClone={handleClone}
                onPublish={publishDeck}
                ownedCloneId={myDecks.find((item) => item.sourceDeckId === deck.id)?.id}
                showStudyAction={false}
              />
            ))}
          </div>
          <Pagination onChange={setPage} page={page} totalPages={totalPages} />
        </>
      ) : (
        <EmptyState text="Попробуйте другой запрос или сбросьте фильтры." title="Ничего не найдено" />
      )}
    </div>
  )
}
