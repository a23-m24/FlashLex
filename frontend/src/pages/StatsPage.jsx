import { AlertTriangle, BarChart3, CalendarDays, CheckCircle2 } from 'lucide-react'
import { useMemo } from 'react'
import { useFlashLex } from '../features/auth/useFlashLex'
import { getAccuracy, getWeakCards } from '../shared/lib/metrics'
import { getDeckQueue, groupCardsByDeck, mapProgressByCard } from '../shared/lib/queue'
import { Badge } from '../shared/ui/Badge'
import { EmptyState } from '../shared/ui/EmptyState'
import { StatTile } from '../shared/ui/StatTile'

const dayAccuracy = (day) => {
  const total = (day.learned || 0) + (day.reviewed || 0)
  return total ? Math.round(((day.correct || 0) / total) * 100) : 0
}

export function StatsPage() {
  const { user, decks, flashcards, progress, dailyStats, dailyStatsHistory } = useFlashLex()
  const myDecks = useMemo(
    () => decks.filter((deck) => deck.authorId === user.id),
    [decks, user.id],
  )
  const myDeckIds = useMemo(
    () => new Set(myDecks.map((deck) => deck.id)),
    [myDecks],
  )
  const myCards = useMemo(
    () => flashcards.filter((card) => myDeckIds.has(card.deckId)),
    [flashcards, myDeckIds],
  )
  const myCardIds = useMemo(
    () => new Set(myCards.map((card) => card.id)),
    [myCards],
  )
  const myProgress = useMemo(
    () => progress.filter((item) => myCardIds.has(item.flashcardId)),
    [myCardIds, progress],
  )
  const cardsByDeckId = useMemo(
    () => groupCardsByDeck(flashcards),
    [flashcards],
  )
  const progressByCardId = useMemo(
    () => mapProgressByCard(progress),
    [progress],
  )
  const deckRows = useMemo(
    () =>
      myDecks
        .map((deck) => ({ deck, queue: getDeckQueue(deck.id, cardsByDeckId, progressByCardId) }))
        .sort((left, right) => {
          const leftDue = left.queue.learningCount + left.queue.reviewCount
          const rightDue = right.queue.learningCount + right.queue.reviewCount
          return rightDue - leftDue || left.deck.name.localeCompare(right.deck.name)
        }),
    [cardsByDeckId, myDecks, progressByCardId],
  )
  const todayQueue = useMemo(
    () =>
      deckRows.reduce(
        (counts, row) => ({
          newCount: counts.newCount + row.queue.newCount,
          learningCount: counts.learningCount + row.queue.learningCount,
          reviewCount: counts.reviewCount + row.queue.reviewCount,
        }),
        { newCount: 0, learningCount: 0, reviewCount: 0 },
      ),
    [deckRows],
  )
  const weakCards = useMemo(
    () => getWeakCards(myCards, myProgress).slice(0, 8),
    [myCards, myProgress],
  )
  const weekStats = useMemo(
    () => (dailyStatsHistory.length ? dailyStatsHistory : [dailyStats]),
    [dailyStats, dailyStatsHistory],
  )

  return (
    <div className="page-stack">
      <section className="page-hero">
        <div>
          <span className="eyebrow">Статистика</span>
          <h1>Состояние обучения</h1>
          <p>Очередь на сегодня, последние 7 дней, наборы и слабые слова.</p>
        </div>
      </section>

      <div className="stats-grid">
        <StatTile icon={CalendarDays} label="Новые" tone="blue" value={todayQueue.newCount} />
        <StatTile icon={AlertTriangle} label="Изучаемые" tone="red" value={todayQueue.learningCount} />
        <StatTile icon={CheckCircle2} label="Повторить" tone="green" value={todayQueue.reviewCount} />
        <StatTile icon={BarChart3} label="Точность" tone="amber" value={`${getAccuracy(myProgress)}%`} />
      </div>

      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Последние 7 дней</h2>
            <p>Новые слова, повторения, точность и очки рейтинга.</p>
          </div>
        </div>
        <div className="table-wrap">
          <table className="data-table stats-week-table">
            <thead>
              <tr>
                <th>День</th>
                <th>Новые</th>
                <th>Повторения</th>
                <th>Точность</th>
                <th>Очки</th>
              </tr>
            </thead>
            <tbody>
              {weekStats.map((day) => (
                <tr key={day.date}>
                  <td>{day.date}</td>
                  <td>{day.learned}</td>
                  <td>{day.reviewed}</td>
                  <td>{dayAccuracy(day)}%</td>
                  <td>{day.points}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Наборы</h2>
            <p>Сколько слов находится в каждом буфере.</p>
          </div>
        </div>
        <div className="card-table">
          {deckRows.map(({ deck, queue }) => (
            <article className="stats-deck-row" key={deck.id}>
              <div>
                <strong>{deck.name}</strong>
                <span>{queue.cards.length} карточек</span>
              </div>
              <div className="stats-buffer-counters">
                <span className="stats-buffer-counters__new">{queue.newCount}</span>
                <span className="stats-buffer-counters__learning">{queue.learningCount}</span>
                <span className="stats-buffer-counters__review">{queue.reviewCount}</span>
              </div>
              <div className="tag-row">
                <Badge tone={deck.isPublished ? 'green' : 'neutral'}>
                  {deck.isPublished ? 'Публичный' : 'Личный'}
                </Badge>
                <Badge tone="blue">{deck.level}</Badge>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Слабые слова</h2>
            <p>Карточки с высокой долей ошибок.</p>
          </div>
        </div>
        {weakCards.length ? (
          <div className="card-table">
            {weakCards.map(({ card, progress: cardProgress, wrongRate, answers }) => (
              <article className="word-row word-row--weak" key={card.id}>
                <div>
                  <strong>{card.englishWord}</strong>
                  <span>{card.translation}</span>
                </div>
                <div>
                  <strong>{Math.round(wrongRate * 100)}% ошибок</strong>
                  <span>{cardProgress?.wrongAnswers || 0} ошибок из {answers} ответов</span>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState text="Пока нет карточек с заметной долей ошибок." title="Слабых слов нет" />
        )}
      </section>
    </div>
  )
}
