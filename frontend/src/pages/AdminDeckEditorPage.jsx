import { useEffect, useMemo, useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { useFlashLex } from '../features/auth/useFlashLex'
import { DeckForm } from '../features/decks/DeckForm'
import { flashlexApi, normalizeCard, normalizeDeck } from '../shared/api/flashlexApi'
import { EmptyState } from '../shared/ui/EmptyState'
import { LinkButton } from '../shared/ui/LinkButton'

export function AdminDeckEditorPage() {
  const { deckId } = useParams()
  const { user } = useFlashLex()
  const navigate = useNavigate()
  const [deckResponse, setDeckResponse] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    flashlexApi.deck(deckId)
      .then((response) => {
        if (!cancelled) {
          if (!response.published) {
            setError('Набор недоступен для редактирования.')
            return
          }
          setDeckResponse(response)
        }
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message)
      })

    return () => {
      cancelled = true
    }
  }, [deckId])

  const deck = useMemo(
    () => (deckResponse ? normalizeDeck(deckResponse) : null),
    [deckResponse],
  )
  const cards = useMemo(
    () => (deckResponse?.cards || []).map((card) => normalizeCard(card, deckResponse.id)),
    [deckResponse],
  )

  if (user.role !== 'admin') {
    return <Navigate replace to="/" />
  }

  const handleSubmit = async (form) => {
    await flashlexApi.adminUpdateDeck(deckId, {
      ...form,
      isPublished: true,
    })
    navigate('/admin')
  }

  if (error) {
    return (
      <section className="panel centered-panel">
        <h1>Набор недоступен</h1>
        <p className="field__error">{error}</p>
        <LinkButton to="/admin" variant="secondary">Вернуться</LinkButton>
      </section>
    )
  }

  if (!deck) {
    return (
      <section className="panel centered-panel">
        <h1>Загрузка набора</h1>
      </section>
    )
  }

  return (
    <div className="page-stack">
      <section className="page-hero">
        <div>
          <span className="eyebrow">Администрирование</span>
          <h1>Изменение набора</h1>
        </div>
      </section>
      <EmptyState title={deck.name} text={`Автор: ${deck.authorName}`} />
      <DeckForm
        initialCards={cards}
        initialDeck={{ ...deck, isPublished: true }}
        onSubmit={handleSubmit}
        publicationDisabled
      />
    </div>
  )
}
