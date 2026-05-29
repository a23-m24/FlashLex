import { useNavigate, useParams } from 'react-router-dom'
import { useFlashLex } from '../features/auth/useFlashLex'
import { DeckForm } from '../features/decks/DeckForm'
import { getDeckCards } from '../shared/lib/metrics'

export function DeckEditorPage() {
  const { deckId } = useParams()
  const { user, decks, flashcards, createDeck, updateDeck } = useFlashLex()
  const navigate = useNavigate()
  const deck = deckId ? decks.find((item) => item.id === deckId) : null
  const cards = deck ? getDeckCards(flashcards, deck.id) : []

  const handleSubmit = async (form) => {
    if (deck) {
      await updateDeck(deck.id, form)
      navigate(`/decks/${deck.id}`)
      return
    }

    const newDeckId = await createDeck(form)
    navigate(`/decks/${newDeckId}`)
  }

  return (
    <div className="page-stack">
      <DeckForm
        initialCards={cards}
        initialDeck={deck}
        onSubmit={handleSubmit}
        publicationDisabled={Boolean(user.publicationBanned && !deck?.isPublished)}
        publicationHint={
          user.publicationBanned && !deck?.isPublished
            ? 'Администратор временно запретил публикацию ваших наборов.'
            : ''
        }
      />
    </div>
  )
}
