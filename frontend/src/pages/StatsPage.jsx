import { Activity, CheckCircle2, Clock, Target } from 'lucide-react'
import { ProgressSummary } from '../features/stats/ProgressSummary'
import { TagProgressChart } from '../features/stats/TagProgressChart'
import { WeakWordsList } from '../features/stats/WeakWordsList'
import { useFlashLex } from '../features/auth/useFlashLex'
import {
  getAccuracy,
  getProgress,
  getStatusSummary,
  getWeakCards,
} from '../shared/lib/metrics'
import { StatTile } from '../shared/ui/StatTile'

const buildTagProgress = (decks, cards, progress) => {
  const map = {}

  decks.forEach((deck) => {
    const deckCards = cards.filter((card) => card.deckId === deck.id)
    const review = deckCards.filter((card) => {
      const status = getProgress(progress, card.id)?.status
      return status === 'REVIEW' || status === 'GRADUATED'
    }).length
    const tags = deck.tags || []

    tags.forEach((tag) => {
      if (!map[tag]) {
        map[tag] = { name: tag, total: 0, graduated: 0 }
      }
      map[tag].total += deckCards.length
      map[tag].graduated += review
    })
  })

  return Object.values(map).map((tag) => ({
    ...tag,
    percent: tag.total ? Math.round((tag.graduated / tag.total) * 100) : 0,
  }))
}

export function StatsPage() {
  const { user, decks, flashcards, progress, dailyStats } = useFlashLex()
  const myDecks = decks.filter((deck) => deck.authorId === user.id)
  const myDeckIds = myDecks.map((deck) => deck.id)
  const myCards = flashcards.filter((card) => myDeckIds.includes(card.deckId))
  const myProgress = progress.filter((item) =>
    myCards.some((card) => card.id === item.flashcardId),
  )
  const summary = getStatusSummary(myCards, myProgress)
  const weakCards = getWeakCards(myCards, myProgress)
  const tags = buildTagProgress(myDecks, myCards, myProgress)
  const dueToday = myProgress.filter((item) => item.nextReviewDate <= dailyStats.date).length

  return (
    <div className="page-stack">
      <section className="page-hero">
        <div>
          <span className="eyebrow">Аналитика</span>
          <h1>Статистика обучения</h1>
          <p>Прогресс по карточкам, тэгам, качеству ответов и повторениям.</p>
        </div>
      </section>

      <div className="stats-grid">
        <StatTile icon={Target} label="Карточек" value={myCards.length} />
        <StatTile icon={CheckCircle2} label="Точность" tone="green" value={`${getAccuracy(myProgress)}%`} />
        <StatTile icon={Clock} label="На сегодня" tone="amber" value={dueToday} />
        <StatTile icon={Activity} label="Ответов" tone="red" value={dailyStats.reviewed} />
      </div>

      <div className="dashboard-grid">
        <ProgressSummary summary={summary} />
        <WeakWordsList items={weakCards} />
      </div>

      <TagProgressChart tags={tags} />
    </div>
  )
}
