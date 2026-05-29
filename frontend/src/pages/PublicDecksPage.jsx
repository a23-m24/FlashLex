import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useFlashLex } from '../features/auth/useFlashLex'
import { DeckCard } from '../features/decks/DeckCard'
import { DeckFilters } from '../features/decks/DeckFilters'
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
  const {
    user,
    decks,
    flashcards,
    cloneDeck,
    loadPublicDeckFacets,
    loadPublicDecks,
    publishDeck,
  } = useFlashLex()
  const navigate = useNavigate()
  const [page, setPage] = useState(1)
  const [catalogPage, setCatalogPage] = useState({
    decks: [],
    totalPages: 1,
    totalElements: 0,
  })
  const [facets, setFacets] = useState({
    levels: [],
    tags: [],
  })
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [filters, setFilters] = useState({
    query: '',
    tag: '',
    level: '',
    sort: 'rating',
  })
  const myDecks = useMemo(
    () => decks.filter((deck) => deck.authorId === user.id),
    [decks, user.id],
  )
  const publicDecks = useMemo(
    () => catalogPage.decks.map((deck) => decks.find((item) => item.id === deck.id) || deck),
    [catalogPage.decks, decks],
  )
  const tags = facets.tags
  const levels = facets.levels

  useEffect(() => {
    let cancelled = false

    loadPublicDeckFacets()
      .then((response) => {
        if (!cancelled) {
          setFacets({
            levels: response.levels || [],
            tags: response.tags || [],
          })
        }
      })
      .catch(() => undefined)

    return () => {
      cancelled = true
    }
  }, [loadPublicDeckFacets])

  useEffect(() => {
    let cancelled = false

    loadPublicDecks({
      ...filters,
      page: page - 1,
      size: PAGE_SIZE,
    })
      .then((response) => {
        if (!cancelled) {
          setCatalogPage({
            decks: response.decks,
            totalPages: response.totalPages || 1,
            totalElements: response.totalElements || response.decks.length,
          })
        }
      })
      .catch((requestError) => {
        if (!cancelled) {
          setError(requestError.message)
          setCatalogPage({ decks: [], totalPages: 1, totalElements: 0 })
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [filters, loadPublicDecks, page])

  const totalPages = Math.max(1, catalogPage.totalPages)
  const visibleDecks = publicDecks

  const handleFilters = (nextFilters) => {
    setIsLoading(true)
    setError('')
    setFilters(nextFilters)
    setPage(1)
  }

  const handlePageChange = (nextPage) => {
    setIsLoading(true)
    setError('')
    setPage(nextPage)
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
          <p>Поиск по названию, тегам, уровню и автору.</p>
        </div>
      </section>

      <DeckFilters
        filters={filters}
        levels={levels}
        onChange={handleFilters}
        sortOptions={sortOptions}
        tags={tags}
      />

      {error ? (
        <EmptyState text={error} title="Не удалось загрузить каталог" />
      ) : isLoading ? (
        <EmptyState text="Загружаем опубликованные наборы." title="Загрузка каталога" />
      ) : visibleDecks.length ? (
        <>
          <div className="deck-grid">
            {visibleDecks.map((deck) => (
              <DeckCard
                canPublish={!user.publicationBanned}
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
          <Pagination onChange={handlePageChange} page={page} totalPages={totalPages} />
        </>
      ) : (
        <EmptyState text="Попробуйте другой запрос или сбросьте фильтры." title="Ничего не найдено" />
      )}
    </div>
  )
}
