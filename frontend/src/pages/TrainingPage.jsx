import { Play } from 'lucide-react'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { TrainingModeSelector } from '../features/training/TrainingModeSelector'
import { useFlashLex } from '../features/auth/useFlashLex'
import { LinkButton } from '../shared/ui/LinkButton'
import { Select } from '../shared/ui/Select'

export function TrainingPage() {
  const [searchParams] = useSearchParams()
  const { user, decks } = useFlashLex()
  const availableDecks = decks.filter((deck) => deck.authorId === user.id)
  const initialDeckId = searchParams.get('deck') || availableDecks[0]?.id || ''
  const [deckId, setDeckId] = useState(initialDeckId)
  const [directionMode, setDirectionMode] = useState('DIRECT_TRANSLATION')
  const [answerMode, setAnswerMode] = useState('SELF_CHECK')

  const startUrl = `/training/session?deck=${deckId}&direction=${directionMode}&answer=${answerMode}`

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
        <Select onChange={(event) => setDeckId(event.target.value)} value={deckId}>
          {availableDecks.map((deck) => (
            <option key={deck.id} value={deck.id}>
              {deck.name}
            </option>
          ))}
        </Select>
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
